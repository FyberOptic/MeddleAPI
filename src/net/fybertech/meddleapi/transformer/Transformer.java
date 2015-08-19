package net.fybertech.meddleapi.transformer;

import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.fybertech.dynamicmappings.DynamicMappings;
import net.fybertech.meddle.MeddleUtil;
import net.fybertech.meddleapi.MeddleAPI;
import net.minecraft.launchwrapper.IClassTransformer;

public class Transformer implements IClassTransformer 
{

	String minecraftServer = "net.minecraft.server.MinecraftServer";	
	String dedicatedServerClass = null;
	String startServer_name = null;
	
	
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) 
	{		
		if (name.equals(minecraftServer)) return transformMinecraftServer(basicClass);
		if (dedicatedServerClass != null && dedicatedServerClass.equals(name)) return transformDedicatedServer(basicClass);
		return basicClass;
	}

	
	private byte[] transformMinecraftServer(byte[] basicClass)
	{
		if (MeddleUtil.isClientJar()) return basicClass;
		
		ClassReader reader = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		reader.accept(cn,  0);	
		
		for (MethodNode method : cn.methods) {
			if (method.name.equals("main") && method.desc.equals("([Ljava/lang/String;)V")) {
				for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) 
				{
					if (insn.getOpcode() != Opcodes.NEW) continue;				
					TypeInsnNode tn = (TypeInsnNode)insn;			
					
					if (DynamicMappings.searchConstantPoolForStrings(tn.desc, "Loading properties", "spawn-protection")) {
						dedicatedServerClass = tn.desc;
						break;
					}
				}
			}
			if (dedicatedServerClass != null) break;
		}	
		
		if (dedicatedServerClass == null) {
			MeddleAPI.LOGGER.error("[MeddleAPI] Error locating DedicatedServer class!");
			return basicClass;
		}
		
		
		// Find DedicatedServer.startServer() in MinecraftServer.run()
		List<MethodNode> methods = DynamicMappings.getMatchingMethods(cn, "run", "()V");
		if (methods.size() == 1) {
			for (AbstractInsnNode insn = methods.get(0).instructions.getFirst(); insn != null; insn = insn.getNext()) {
				if (insn.getOpcode() != Opcodes.INVOKEVIRTUAL) continue;
				MethodInsnNode mn = (MethodInsnNode)insn;
				if (mn.desc.equals("()Z")) {
					startServer_name = mn.name;
					break;
				}
			}
		}
		
		return basicClass;
	}
	
	
	
	private byte[] transformDedicatedServer(byte[] basicClass)
	{
		ClassReader reader = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		reader.accept(cn,  0);	
		
		
		List<MethodNode> methods = DynamicMappings.getMatchingMethods(cn, startServer_name, "()Z");
		if (methods.size() != 1) {
			MeddleAPI.LOGGER.error("[MeddleAPI] Unable to locate DedicatedServer.startServer()!");
			return basicClass;
		}
		
		MethodNode startServer = methods.get(0);
		
		boolean patchedPreInit = false;
		boolean foundNanoTime = false;
		
		boolean patchedInit = false;
		
		// Start at the bottom
		for (AbstractInsnNode insn = startServer.instructions.getLast(); insn != null; insn = insn.getPrevious()) 
		{			
			if (!patchedPreInit && !foundNanoTime && insn.getOpcode() == Opcodes.INVOKESTATIC) {
				MethodInsnNode mn = (MethodInsnNode)insn;
				if (mn.owner.equals("java/lang/System") && mn.name.equals("nanoTime")) foundNanoTime = true;
			}
			else if (!patchedPreInit && foundNanoTime && insn.getOpcode() == Opcodes.NEW) {
				if (insn.getPrevious() != null && insn.getPrevious().getOpcode() == Opcodes.ALOAD) {
					InsnList list = new InsnList();
					list.add(new VarInsnNode(Opcodes.ALOAD, 0));					
					list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/fybertech/meddleapi/MeddleAPI", "preInit", "(Ljava/lang/Object;)V", false));
					list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/fybertech/meddleapi/MeddleAPI", "init", "()V", false));
					startServer.instructions.insertBefore(insn.getPrevious(), list);
					patchedPreInit = true;
				}
			}
			
			if (!patchedInit && insn.getOpcode() == Opcodes.IRETURN) {
				if (insn.getPrevious() != null && insn.getPrevious().getOpcode() == Opcodes.ICONST_1) {
					// TODO
					patchedInit = true;
				}
			}
			
			
		}
		
		
		
		
		//  INVOKESTATIC java/lang/System.nanoTime ()J
		
		
		
		ClassWriter writer = new ClassWriter(0);
		cn.accept(writer);
		return writer.toByteArray();		
	}
	
}
