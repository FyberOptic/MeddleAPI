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
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
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
	String entityPlayerSP = DynamicMappings.getClassMapping("net/minecraft/client/entity/EntityPlayerSP");
	String netClientHandler = DynamicMappings.getClassMapping("net/minecraft/client/network/NetHandlerPlayClient");
	String keyBinding = DynamicMappings.getClassMapping("net/minecraft/client/settings/KeyBinding");
	String blockRendererDispatcher = DynamicMappings.getClassMapping("net/minecraft/client/renderer/BlockRendererDispatcher");
	
	String iBlockState = DynamicMappings.getClassMapping("net/minecraft/block/state/IBlockState");
	String blockPos = DynamicMappings.getClassMapping("net/minecraft/util/BlockPos");
	String iBlockAccess = DynamicMappings.getClassMapping("net/minecraft/world/IBlockAccess");
	String worldRenderer = DynamicMappings.getClassMapping("net/minecraft/client/renderer/WorldRenderer");
	
	
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
		if (name.equals(entityPlayerSP)) return transformEntityPlayerSP(basicClass);
		if (name.equals(netClientHandler)) return transformNetClientHandler(basicClass);
		if (name.equals(keyBinding)) return transformKeyBinding(basicClass);
		if (name.equals(blockRendererDispatcher)) return transformBlockRendererDispatcher(basicClass);
		
		return basicClass;
	}
	
	
	private byte[] transformBlockRendererDispatcher(byte[] basicClass)
	{
		ClassReader reader = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		reader.accept(cn, ClassReader.EXPAND_FRAMES);
		
		MethodNode renderBlock = DynamicMappings.getMethodNodeFromMapping(cn, "net/minecraft/client/renderer/BlockRendererDispatcher renderBlock (Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/BlockPos;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/WorldRenderer;)Z");
		if (renderBlock == null) return basicClass; // TODO - Error message
		if (!MeddleUtil.notNull(iBlockState, blockPos, iBlockAccess, worldRenderer)) return basicClass; // TODO - Error message
		
		String hookDesc = "(L" + iBlockState + ";L" + blockPos + ";L" + iBlockAccess + ";L" + worldRenderer + ";)Z";
		
		InsnList list = new InsnList();		
		list.add(new VarInsnNode(Opcodes.ALOAD, 1));
		list.add(new VarInsnNode(Opcodes.ALOAD, 2));
		list.add(new VarInsnNode(Opcodes.ALOAD, 3));
		list.add(new VarInsnNode(Opcodes.ALOAD, 4));
		list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/fybertech/meddleapi/MeddleClientHooks", "renderBlockHook", hookDesc, false));
		LabelNode l1 = new LabelNode();
		list.add(new JumpInsnNode(Opcodes.IFEQ, l1));
		list.add(new InsnNode(Opcodes.ICONST_1));
		list.add(new InsnNode(Opcodes.IRETURN));
		list.add(l1);			
		
		renderBlock.instructions.insert(list);
		
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cn.accept(writer);
		return writer.toByteArray();
	}
	
	
	private byte[] transformKeyBinding(byte[] basicClass) 
	{
		ClassReader reader = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		reader.accept(cn, ClassReader.EXPAND_FRAMES);	

		MethodNode onTick = DynamicMappings.getMethodNodeFromMapping(cn, "net/minecraft/client/settings/KeyBinding onTick (I)V");
		MethodNode setKeyBindState = DynamicMappings.getMethodNodeFromMapping(cn, "net/minecraft/client/settings/KeyBinding setKeyBindState (IZ)V");
		
		if (setKeyBindState != null) {
			InsnList list = new InsnList();
			list.add(new VarInsnNode(Opcodes.ILOAD, 0));
			list.add(new VarInsnNode(Opcodes.ILOAD, 1));
			list.add(new VarInsnNode(Opcodes.ALOAD, 2));
			list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/fybertech/meddleapi/MeddleClientHooks", "setKeyBindStateHook", "(IZL" + keyBinding + ";)V", false));
			
			AbstractInsnNode returnNode = null;
			for (AbstractInsnNode insn = setKeyBindState.instructions.getLast(); insn != null; insn = insn.getPrevious()) {
				if (insn.getOpcode() == Opcodes.RETURN) {
					returnNode = insn;
					break;
				}
			}
			if (returnNode != null) setKeyBindState.instructions.insertBefore(returnNode, list);
		}
		
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cn.accept(writer);
		return writer.toByteArray();
	}
	
	
	private byte[] transformNetClientHandler(byte[] basicClass)
	{
		ClassReader reader = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		reader.accept(cn, ClassReader.EXPAND_FRAMES);		
		
		MethodNode handleOpenWindow = DynamicMappings.getMethodNodeFromMapping(cn, "net/minecraft/client/network/NetHandlerPlayClient handleOpenWindow (Lnet/minecraft/network/play/server/S2DPacketOpenWindow;)V");
		String packet = DynamicMappings.getClassMapping("net/minecraft/network/play/server/S2DPacketOpenWindow");
		if (handleOpenWindow == null || packet == null || entityPlayerSP == null) { /* TODO: Error */ return basicClass; }
		
		LabelNode endlabel = new LabelNode();
		InsnList list = new InsnList();
		list.add(new VarInsnNode(Opcodes.ALOAD, 2));
		list.add(new VarInsnNode(Opcodes.ALOAD, 1));
		list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/fybertech/meddleapi/MeddleClientHooks", "handleOpenWindowHook", "(L" + entityPlayerSP + ";L" + packet + ";)Z", false));
		list.add(new JumpInsnNode(Opcodes.IFEQ, endlabel));
		list.add(new InsnNode(Opcodes.RETURN));
		list.add(endlabel);
		
		AbstractInsnNode destNode = null;
		for (destNode = handleOpenWindow.instructions.getFirst(); destNode != null; destNode = destNode.getNext()) {
			if (destNode.getOpcode() == Opcodes.ASTORE) break;
		}
		
		if (destNode != null) handleOpenWindow.instructions.insert(destNode, list);
		
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cn.accept(writer);
		return writer.toByteArray();
	}
	
	
	
	private byte[] transformEntityPlayerSP(byte[] basicClass)
	{
		ClassReader reader = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		reader.accept(cn,  0);		
		
		String iInteractionObject = DynamicMappings.getClassMapping("net/minecraft/world/IInteractionObject");
		if (iInteractionObject == null) { /* TODO - error */ return basicClass; }
		
		MethodNode displayGui = DynamicMappings.getMethodNodeFromMapping(cn, "net/minecraft/entity/player/EntityPlayer displayGui (Lnet/minecraft/world/IInteractionObject;)V");
		if (displayGui == null) { /* TODO - error */ return basicClass; }
		
		InsnList list = new InsnList();
		list.add(new VarInsnNode(Opcodes.ALOAD, 0));
		list.add(new VarInsnNode(Opcodes.ALOAD, 1));
		list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/fybertech/meddleapi/MeddleClientHooks", "displayGuiHook", "(L" + cn.name + ";L" + iInteractionObject + ";)V", false));
		
		AbstractInsnNode returnNode = null;
		for (AbstractInsnNode insn = displayGui.instructions.getLast(); insn != null; insn = insn.getPrevious()) {
			if (insn.getOpcode() == Opcodes.RETURN) {
				returnNode = insn;
				break;
			}
		}
		
		if (returnNode != null) displayGui.instructions.insertBefore(returnNode, list);
		
		ClassWriter writer = new ClassWriter(0);
		cn.accept(writer);
		return writer.toByteArray();
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
