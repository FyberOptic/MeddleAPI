package net.fybertech.meddleapi.transformer;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.fybertech.dynamicmappings.DynamicMappings;
import net.fybertech.meddle.Meddle;
import net.fybertech.meddle.MeddleUtil;
import net.fybertech.meddleapi.MeddleAPI;
import net.minecraft.launchwrapper.IClassTransformer;

public class ClientTransformer implements IClassTransformer
{
	
	String minecraftClass = DynamicMappings.getClassMapping("net/minecraft/client/Minecraft");
	String guiMainMenu = DynamicMappings.getClassMapping("net/minecraft/client/gui/GuiMainMenu");
	
	
	public ClientTransformer()
	{		
		if (!MeddleUtil.notNull(minecraftClass, guiMainMenu)) 
			MeddleAPI.LOGGER.error("[MeddleAPI] Error obtaining dynamic mappings for client transformer");
	}
	
	
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) 
	{
		//System.out.println(getClass().getClassLoader() + " " + DynamicMappings.classMappings.size());
		if (name.equals(minecraftClass)) return transformMinecraft(basicClass);
		if (name.equals(guiMainMenu)) return transformGuiMainMenu(basicClass);
		
		return basicClass;
	}
	
	
	private byte[] transformGuiMainMenu(byte[] basicClass)
	{		
		ClassReader reader = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		reader.accept(cn,  0);
		
		//System.out.println("GuiMainMenu: " + cn.name);
		
		String guiScreen = DynamicMappings.getClassMapping("net/minecraft/client/gui/GuiScreen");		
		String drawScreenMapping = DynamicMappings.getMethodMapping("net/minecraft/client/gui/GuiScreen drawScreen (IIF)V");		
		MethodNode drawScreen = DynamicMappings.getMethodNode(cn, drawScreenMapping);
		if (drawScreen == null) return basicClass;
		
		boolean matchedFirst = false;
		for (AbstractInsnNode insn = drawScreen.instructions.getLast(); insn != null; insn = insn.getPrevious()) {
			if (!matchedFirst && insn instanceof MethodInsnNode) {
				MethodInsnNode mn = (MethodInsnNode)insn;				
				if (drawScreenMapping.equals(mn.owner + " " + mn.name + " " + mn.desc)) {
					matchedFirst = true;
					continue;
				}
			}
			if (matchedFirst && insn.getOpcode() == Opcodes.ALOAD) {
				InsnList list = new InsnList();
				list.add(new VarInsnNode(Opcodes.ALOAD, 0));
				list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/fybertech/meddleapi/ClientProxy", "drawMainMenuBranding", "(L" + guiScreen + ";)V", false));
				drawScreen.instructions.insertBefore(insn,  list);
				break;
			}
		}
		
		ClassWriter writer = new ClassWriter(0);
		cn.accept(writer);
		return writer.toByteArray();
	}
	
	
	
	
	private byte[] transformMinecraft(byte[] basicClass)
	{
		ClassReader reader = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		reader.accept(cn,  0);
		
		
		// private void startGame() throws LWJGLException
		
		boolean finished = false;
		for (MethodNode method : (List<MethodNode>)cn.methods) {
			//if (!DynamicMappings.checkMethodParameters(method, Type.OBJECT)) continue;
			Type t = Type.getMethodType(method.desc);
			if (t.getReturnType().getSort() != Type.VOID) continue;			
			if (t.getArgumentTypes().length != 0) continue;
			
			boolean foundFirst = false;				
			for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) 
			{
				// TODO: Add a default resource pack while here
				
				if (!foundFirst && DynamicMappings.isLdcWithString(insn, "Startup")) 
				{
					foundFirst = true;					
					if (insn.getNext() instanceof MethodInsnNode) {
						insn = insn.getNext();
						insn = insn.getNext();
						InsnList list = new InsnList();
						list.add(new VarInsnNode(Opcodes.ALOAD, 0));
						list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/fybertech/meddleapi/MeddleAPI", "preInit", "(Ljava/lang/Object;)V", false));
						method.instructions.insert(insn, list);
					}
					else { /* TODO: Error */ }
					continue;
				}
				
				if (foundFirst && DynamicMappings.isLdcWithString(insn, "Post startup"))
				{
					if (insn.getNext() instanceof MethodInsnNode) {
						insn = insn.getNext();
						// Just use a list here too in case we want to add more later
						InsnList list = new InsnList();						
						list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/fybertech/meddleapi/MeddleAPI", "init", "()V", false));
						method.instructions.insert(insn, list);
						
						finished = true;
					}
					else { /* TODO: Error */ }
					break;
				}
								
				//if (!foundSecond && !DynamicMappings.isLdcWithString(insn, "Post startup")) continue;
				//foundSecond = true;					
			}
			
			if (finished) break;
		}
		
				
		ClassWriter writer = new ClassWriter(0); 
		cn.accept(writer);
		return writer.toByteArray();	
	}
	
	
	

}
