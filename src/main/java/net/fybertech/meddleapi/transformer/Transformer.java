package net.fybertech.meddleapi.transformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.fybertech.dynamicmappings.DynamicMappings;
import net.fybertech.dynamicmappings.InheritanceMap;
import net.fybertech.dynamicmappings.InheritanceMap.FieldHolder;
import net.fybertech.dynamicmappings.InheritanceMap.MethodHolder;
import net.fybertech.meddle.Meddle;
import net.fybertech.meddle.MeddleUtil;
import net.fybertech.meddleapi.MeddleAPI;
import net.minecraft.launchwrapper.IClassTransformer;

public class Transformer implements IClassTransformer 
{

	// TODO - Implement better system for access transformations
	
	
	String minecraftServer = "net.minecraft.server.MinecraftServer";
	String blockClass = DynamicMappings.getClassMapping("net/minecraft/block/Block");
	String itemClass = DynamicMappings.getClassMapping("net/minecraft/item/Item");
	String slotClass = DynamicMappings.getClassMapping("net/minecraft/inventory/Slot");
	String dedicatedServerClass = null;
	String startServer_name = null;
	
	String containerWorkbench = DynamicMappings.getClassMapping("net/minecraft/inventory/ContainerWorkbench");
	String entityPlayerMP = DynamicMappings.getClassMapping("net/minecraft/entity/player/EntityPlayerMP");
	

	
	
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) 
	{		
		if (name.equals(minecraftServer)) basicClass = transformMinecraftServer(basicClass);
		else if (dedicatedServerClass != null && dedicatedServerClass.equals(name)) basicClass = transformDedicatedServer(basicClass);
		//if (blockClass != null && name.equals(blockClass)) return transformBlock(basicClass);
		//if (itemClass != null && name.equals(itemClass)) return transformItem(basicClass);
		//if (slotClass != null && name.equals(slotClass)) return transformSlot(basicClass);
		else if (containerWorkbench != null && containerWorkbench.equals(name)) basicClass = transformContainerWorkbench(basicClass);
		else if (entityPlayerMP != null && entityPlayerMP.equals(name)) basicClass = transformEntityPlayerMP(basicClass);
		
		return basicClass;
	}
	
	
	private byte[] failGracefully(String error, byte[] bytes)
	{
		Meddle.LOGGER.error("[MeddleAPI] " + error);
		return bytes;
	}
	
	

	
	
	/*private byte[] transformSlot(byte[] basicClass)
	{
		ClassReader reader = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		reader.accept(cn,  0);			
		
		for (FieldNode field : cn.fields) {
			if (field.desc.equals("I")) {
				field.access = (field.access & ~Opcodes.ACC_PRIVATE) | Opcodes.ACC_PUBLIC; 
			}
		}
		
		ClassWriter writer = new ClassWriter(0);
		cn.accept(writer);
		return writer.toByteArray();		
	}
	
	
	private byte[] transformBlock(byte[] basicClass)
	{
		ClassReader reader = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		reader.accept(cn,  0);	
		
		for (MethodNode method : cn.methods) 
		{
			// Make Block constructors public
			if (method.name.equals("<init>")) method.access = (method.access & ~Opcodes.ACC_PROTECTED) | Opcodes.ACC_PUBLIC;			
			
			// Make registration methods public			
			String mapping = DynamicMappings.getReverseMethodMapping(cn.name + " " + method.name + " " + method.desc);
			if (mapping != null && mapping.startsWith("net/minecraft/block/Block registerBlock ")) {
				method.access = (method.access & ~Opcodes.ACC_PRIVATE) | Opcodes.ACC_PUBLIC;	
			}			 
		}	
		
		ClassWriter writer = new ClassWriter(0);
		cn.accept(writer);
		return writer.toByteArray();		
	}
	
	
	private byte[] transformItem(byte[] basicClass)
	{
		ClassReader reader = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		reader.accept(cn,  0);	
		
		int allAccess = Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE;
		
		for (MethodNode method : cn.methods) 
		{			
			// Make registration methods public			
			String mapping = DynamicMappings.getReverseMethodMapping(cn.name + " " + method.name + " " + method.desc);
			if (mapping != null && mapping.startsWith("net/minecraft/item/Item registerItem")) {
				method.access = (method.access & ~allAccess) | Opcodes.ACC_PUBLIC;	
			}			 
		}	
		
		ClassWriter writer = new ClassWriter(0);
		cn.accept(writer);
		return writer.toByteArray();		
	}*/

	
	private byte[] transformMinecraftServer(byte[] basicClass)
	{
		ClassReader reader = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		reader.accept(cn,  0);	
		
		
		// Find DedicatedServer.startServer() in MinecraftServer.run()
		List<MethodNode> methods = DynamicMappings.getMatchingMethods(cn, "run", "()V");
		if (methods.size() == 1) 
		{			
			MethodNode method = methods.get(0);			
			for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
				if (insn.getOpcode() != Opcodes.INVOKEVIRTUAL) continue;
				MethodInsnNode mn = (MethodInsnNode)insn;
				if (mn.owner.equals("net/minecraft/server/MinecraftServer") && mn.desc.equals("()Z")) {
					startServer_name = mn.name;
					break;
				}
			}
			
			InsnList list = new InsnList();
			list.add(new VarInsnNode(Opcodes.ALOAD, 0));
			list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/fybertech/meddleapi/MeddleAPI", "onServerRunHook", "(Lnet/minecraft/server/MinecraftServer;)V", false));
			
			method.instructions.insertBefore(method.instructions.getFirst(), list); 
					 
		}
		
		
		if (!MeddleUtil.isClientJar())
		{
			for (MethodNode method : (List<MethodNode>)cn.methods) {
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
			}
		}
				
		
		ClassWriter writer = new ClassWriter(0);
		cn.accept(writer);
		return writer.toByteArray();		
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
	
	
	
	private byte[] transformContainerWorkbench(byte[] basicClass)
	{
		ClassReader reader = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		reader.accept(cn,  0);	
		
		MethodNode onChanged = DynamicMappings.getMethodNodeFromMapping(cn, "net/minecraft/inventory/Container onCraftMatrixChanged (Lnet/minecraft/inventory/IInventory;)V");
		if (onChanged == null) return failGracefully("Couldn't locate Container.onCraftMatrixChanged!", basicClass);
		
		String detectAndSend = DynamicMappings.getMethodMapping("net/minecraft/inventory/Container detectAndSendChanges ()V");
		if (detectAndSend == null) return failGracefully("Couldn't locate Container.detectAndSendChanges!", basicClass);
		
		AbstractInsnNode returnNode = null;
		for (AbstractInsnNode insn = onChanged.instructions.getLast(); insn != null; insn = insn.getPrevious()) {
			if (insn.getOpcode() == Opcodes.RETURN) { returnNode = insn; break; }
		}
		
		String container = DynamicMappings.getClassMapping("net/minecraft/inventory/Container");
		
		InsnList list = new InsnList();
		//list.add(new VarInsnNode(Opcodes.ALOAD, 0));
		//list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, cn.name, detectAndSend.split(" ")[1], "()V", false));
		list.add(new VarInsnNode(Opcodes.ALOAD, 0));
		list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/fybertech/meddleapi/MeddleHooks", "containerWorkbenchHook", "(L" + container + ";)V", false));
		
		onChanged.instructions.insertBefore(returnNode, list);
		
		ClassWriter writer = new ClassWriter(0);
		cn.accept(writer);
		return writer.toByteArray();		
	}
	
	
	// Remove restriction on SlotCrafting slots not being updated by EntityPlayerMP.sendSlotContents
	private byte[] transformEntityPlayerMP(byte[] basicClass)
	{
		ClassReader reader = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		reader.accept(cn,  0);	
		
		MethodNode sendSlotContents = DynamicMappings.getMethodNodeFromMapping(cn, "net/minecraft/inventory/ICrafting sendSlotContents (Lnet/minecraft/inventory/Container;ILnet/minecraft/item/ItemStack;)V");
		if (sendSlotContents == null) return failGracefully("Couldn't locate EntityPlayerMP.sendSlotContents!", basicClass);
		
		AbstractInsnNode[] nodes = DynamicMappings.getOpcodeSequenceArray(sendSlotContents.instructions.getFirst(), Opcodes.ALOAD, Opcodes.ILOAD, Opcodes.INVOKEVIRTUAL, Opcodes.INSTANCEOF, Opcodes.IFEQ, Opcodes.RETURN);
		if (nodes == null) return failGracefully("Unable to locate opcode sequence in EntityPlayerMP.sendSlotContents!", basicClass);
		
		for (AbstractInsnNode insn : nodes) {
			sendSlotContents.instructions.remove(insn);
		}
		//System.out.println("Patched EntityPlayerMP.sendSlotContents");
		
		ClassWriter writer = new ClassWriter(0);
		cn.accept(writer);
		return writer.toByteArray();		
	}
	
}
