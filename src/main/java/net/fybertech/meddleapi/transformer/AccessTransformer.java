package net.fybertech.meddleapi.transformer;

import net.fybertech.dynamicmappings.AccessUtil;
import net.fybertech.dynamicmappings.DynamicMappings;
import net.fybertech.dynamicmappings.InheritanceMap;
import net.fybertech.dynamicmappings.InheritanceMap.FieldHolder;
import net.fybertech.dynamicmappings.InheritanceMap.MethodHolder;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AccessTransformer  implements IClassTransformer
{

	/*static String[] fieldTransformers = new String[] {
		"net/minecraft/inventory/Slot * I 1",
		"net/minecraft/item/crafting/ShapedRecipes * * 1",
		"net/minecraft/item/crafting/ShapelessRecipes * * 1"
	};
	
	static String[] methodTransformers = new String[] {
		"net/minecraft/block/Block <init> * 1",
		"net/minecraft/block/Block registerBlock * 1",
		"net/minecraft/item/Item registerItem * 1",
		"net/minecraft/item/Item registerItemBlock * 1",
		"net/minecraft/client/gui/inventory/GuiContainer drawItemStack (Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V 1"
	};*/
	
		
	static boolean processedTransformers = false;
	static Map<String, Integer> expandedFieldTransformers = new HashMap<>();
	static Map<String, Integer> expandedMethodTransformers = new HashMap<>();
	static Set<String> classesToTransform = new HashSet<>();
	
	
	
	String getClassMapping(String deobf) 
	{		
		String result = DynamicMappings.getClassMapping(deobf);
		return result != null ? result : deobf;
	}
	
	String getReverseClassMapping(String obf) 
	{		
		String result = DynamicMappings.getReverseClassMapping(obf);
		return result != null ? result : obf;
	}	
	
	
	String remapFieldDescriptor(String desc) 
	{
		Type t = Type.getType(desc);
		
		String out = "";
		if (t.getSort() != Type.OBJECT) out += t.getDescriptor();
		else out += "L" + getReverseClassMapping(t.getClassName()) + ";";
		
		out = out.replace(".", "/");
		
		return out;
	}
	
	
	String remapMethodDescriptor(String desc) 
	{
		Type t = Type.getMethodType(desc);
		Type[] args = t.getArgumentTypes();
		Type returnType = t.getReturnType();
		
		String out = "(";
		for (Type arg : args) {
			if (arg.getSort() != Type.OBJECT) out += arg.getDescriptor();
			else out += "L" + getReverseClassMapping(arg.getClassName()) + ";";
		}
		out += ")";
		if (returnType.getSort() != Type.OBJECT) out += returnType.getDescriptor();
		else out += "L" + getReverseClassMapping(returnType.getClassName()) + ";";
		
		out = out.replace(".", "/");
		
		return out;
	}
	
		
	
	public AccessTransformer()
	{
		if (processedTransformers) return;
		
		AccessUtil accessUtil = new AccessUtil();
		accessUtil.readAllTransformerConfigs();
		
		InheritanceMap inheritanceMap = new InheritanceMap();		
		
		for (String transformer : accessUtil.accessTransformerFields) {
			String[] split = transformer.split(" ");
			if (split.length != 4) continue;
			
			String className = split[0];
			String fieldName = split[1];
			String fieldDesc = split[2];
			int access = 0;
			try {
				access = Integer.parseInt(split[3]);
			}
			catch (NumberFormatException e) {}
			if (access < 1) continue;
			
			String mappedClass = getClassMapping(className);
			InheritanceMap classMap = null;
			try {
				classMap = inheritanceMap.buildMap(mappedClass);
			} catch (IOException e) { continue; }
			
			ClassNode classNode = DynamicMappings.getClassNode(mappedClass);
			if (classNode == null) continue;			
			
			Map<String, String> fieldsMap = new HashMap<>();
			
			for (String key : classMap.fields.keySet()) 
			{
				// Check if this field has been defined in the specific class for the access transformation
				boolean found = false;
				for (FieldNode field : classNode.fields) {
					if (key.equals(field.name + " " + field.desc)) { found = true; break; }
				}
				// If not, ignore it, you can't change access if it's not there
				if (!found) continue;
				
				Set<FieldHolder> inheritedFields = classMap.fields.get(key);
				
				// Search through inheritance, see if any of them have a mapping
				String fieldMapping = null;
				for (FieldHolder holder : inheritedFields) {
					fieldMapping = DynamicMappings.getReverseFieldMapping(holder.cn.name + " " + holder.fn.name + " " + holder.fn.desc);
					if (fieldMapping != null) break;
				}
				
				// If not, just map the method descriptor
				if (fieldMapping == null) {
					String[] keysplit = key.split(" ");
					fieldMapping = keysplit[0] + " " + remapFieldDescriptor(keysplit[1]);
				}
				else fieldMapping = fieldMapping.substring(fieldMapping.indexOf(' ') + 1);
				
				fieldsMap.put(fieldMapping, key);					
			}
			
			
			// Check for matches with access transformer string
			for (String key : fieldsMap.keySet()) {
				//System.out.println("KEY: " + key + " -> " + methodsMap.get(key));
				split = key.split(" ");
				if (!fieldName.equals(split[0]) && !fieldName.equals("*")) continue;
				if (!fieldDesc.equals(split[1]) && !fieldDesc.equals("*")) continue;
				//System.out.println("MATCHED: " + key + " -> " + fieldsMap.get(key));
				expandedFieldTransformers.put(mappedClass + " " + fieldsMap.get(key), access);
				classesToTransform.add(mappedClass);
			}			
		}
		
		
		for (String transformer : accessUtil.accessTransformerMethods) {
			String[] split = transformer.split(" ");
			if (split.length != 4) continue;
			
			String className = split[0];
			String methodName = split[1];
			String methodDesc = split[2];
			int access = 0;
			try {
				access = Integer.parseInt(split[3]);
			}
			catch (NumberFormatException e) {}
			if (access < 1) continue;
			
			String mappedClass = getClassMapping(className);
			InheritanceMap classMap = null;
			try {
				classMap = inheritanceMap.buildMap(mappedClass);
			} catch (IOException e) { continue; }
			
			ClassNode classNode = DynamicMappings.getClassNode(mappedClass);
			if (classNode == null) continue;			
			
			Map<String, String> methodsMap = new HashMap<>();
			
			for (String key : classMap.methods.keySet()) 
			{
				// Check if this method has been defined in the specific class for the access transformation
				boolean found = false;
				for (MethodNode method : classNode.methods) {
					if (key.equals(method.name + " " + method.desc)) { found = true; break; }
				}
				// If not, ignore it, you can't change access if it's not there
				if (!found) continue;
				
				Set<MethodHolder> inheritedMethods = classMap.methods.get(key);
				
				// Search through inheritance, see if any of them have a mapping
				String methodMapping = null;
				for (MethodHolder holder : inheritedMethods) {
					methodMapping = DynamicMappings.getReverseMethodMapping(holder.cn.name + " " + holder.mn.name + " " + holder.mn.desc);
					if (methodMapping != null) break;
				}
				
				// If not, just map the method descriptor
				if (methodMapping == null) {
					String[] keysplit = key.split(" ");
					methodMapping = keysplit[0] + " " + remapMethodDescriptor(keysplit[1]);
				}
				else methodMapping = methodMapping.substring(methodMapping.indexOf(' ') + 1);
				
				methodsMap.put(methodMapping, key);					
			}
			
			
			// Check for matches with access transformer string
			for (String key : methodsMap.keySet()) {
				//System.out.println("KEY: " + key + " -> " + methodsMap.get(key));
				split = key.split(" ");
				if (!methodName.equals(split[0]) && !methodName.equals("*")) continue;
				if (!methodDesc.equals(split[1]) && !methodDesc.equals("*")) continue;
				//System.out.println("MATCHED: " + key + " -> " + methodsMap.get(key));
				expandedMethodTransformers.put(mappedClass + " " + methodsMap.get(key), access);
				classesToTransform.add(mappedClass);
			}			
		}
		
		processedTransformers = true;
		
	}
	
	
	
	private byte[] transformClass(byte[] basicClass)
	{
		ClassReader reader = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		reader.accept(cn,  0);
		
		//System.out.println("Access transforming " + cn.name);
		
		int allAccess = Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE;
		
		for (FieldNode field : cn.fields) {
			String key = cn.name + " " + field.name + " " + field.desc;
			if (!expandedFieldTransformers.containsKey(key)) continue;
			int access = expandedFieldTransformers.get(key);
			if (access < 1) continue;
			field.access = (field.access & ~allAccess) | access;
		}
		
		for (MethodNode method : cn.methods) {
			String key = cn.name + " " + method.name + " " + method.desc;
			if (!expandedMethodTransformers.containsKey(key)) continue;
			int access = expandedMethodTransformers.get(key);
			if (access < 1) continue;
			method.access = (method.access & ~allAccess) | access;
		}		
		
		ClassWriter writer = new ClassWriter(0);
		cn.accept(writer);
		return writer.toByteArray();	
	}
	
	
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) 
	{
		if (classesToTransform.contains(name)) basicClass = transformClass(basicClass);
		return basicClass;
	}

}
