package net.fybertech.meddleapi.transformer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import net.fybertech.dynamicmappings.DynamicMappings;
import net.fybertech.dynamicmappings.DynamicRemap;
import net.fybertech.dynamicmappings.InheritanceMap;
import net.minecraft.launchwrapper.IClassTransformer;

public class ReobfTransformer implements IClassTransformer
{

	DynamicRemap toObfRemapper = null;

	String[] exclusions = new String[] {
			"com.jcraft.",
			"net.fybertech.meddle.",
			"net.fybertech.dynamicmappings.",
			//"net.fybertech.meddleapi.",
			"org.slf4j.",
			"org.apache.",
			"io.netty.",
			"com.google",
			"paulscode.",
			"joptsimple.",
			"com.mojang.",
			"net.minecraft.",
			"oshi.",
			"com.ibm."
	};



	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{		
		if (!name.contains(".")) return basicClass;
		for (String ex : exclusions) { if (name.startsWith(ex)) return basicClass; }
		
		return toObfRemapper.remapClass(basicClass);
	}

	
	
	public static byte[] readStream(InputStream stream) throws IOException {

	    byte[] buffer = new byte[1024];
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();

	    int bytesRead = 0;
	    while ((bytesRead = stream.read(buffer)) != -1) {
	        baos.write(buffer, 0, bytesRead);
	    }
	    stream.close();
	    baos.flush();
	    baos.close();
	    return baos.toByteArray();
	}


	public ReobfTransformer()
	{
		
		final AccessTransformer accessTransformer = new AccessTransformer();
		
		
		final DynamicRemap toDeobfRemapper = new DynamicRemap(
				DynamicMappings.reverseClassMappings,
				DynamicMappings.reverseFieldMappings,
				DynamicMappings.reverseMethodMappings) {
		
			Map<String, ClassNode> transformedCache = new HashMap<>();		
			
			
			@Override
			public ClassNode remapClass(String className) 
			{
				// TODO - Cache this, otherwise multiple access transformations occur
				
				if (className == null) return null;
				InputStream stream = getClass().getClassLoader().getResourceAsStream(className + ".class");
				
				byte[] data = null;
				try {
					data = readStream(stream);
				} catch (IOException e) {}
				if (data == null) return null;				
				
				ClassReader reader = new ClassReader(accessTransformer.transform(className, className, data));
				
				return remapClass(reader);				
			}
			
			
			@Override
			public ClassNode getClassNode(String className) 
			{
				if (transformedCache.containsKey(className)) return transformedCache.get(className);
				
				ClassNode cn = super.getClassNode(className);
				ClassWriter writer = new ClassWriter(0);
				cn.accept(writer);
				
				byte[] bytes = accessTransformer.transform(cn.name, cn.name, writer.toByteArray());
				
				ClassReader reader = new ClassReader(bytes);
				ClassNode outNode = new ClassNode();
				reader.accept(outNode, 0);
				
				transformedCache.put(className, outNode);
				
				return outNode;
			}			
			
		};

		toDeobfRemapper.unpackagedPrefix = null;
		toDeobfRemapper.unpackagedInnerPrefix = null;
		
		toDeobfRemapper.inheritanceMapper = new InheritanceMap() {
			@Override
			public ClassNode locateClass(String classname) throws IOException
			{
				return toDeobfRemapper.getClassNode(classname);
			}
		};
		

		
		

		toObfRemapper = new DynamicRemap(
				DynamicMappings.classMappings,
				DynamicMappings.fieldMappings,
				DynamicMappings.methodMappings) {

			@Override
			public ClassNode getClassNode(String className) {
				if (className == null) return null;

				className = className.replace(".", "/");

				if (DynamicMappings.classMappings.containsKey(className)) {
					return toDeobfRemapper.remapClass(DynamicMappings.classMappings.get(className));
				}

				return toDeobfRemapper.getClassNode(className);
			}

		};

		toObfRemapper.unpackagedPrefix = null;
		toObfRemapper.unpackagedInnerPrefix = null;

		toObfRemapper.inheritanceMapper = new InheritanceMap() {
			@Override
			public ClassNode locateClass(String classname) throws IOException
			{
				//String out = DynamicMappings.getClassMapping(classname);
				//if (out != null) classname = out;
				//return toDeobfRemapper.remapClass(classname);
				return toObfRemapper.getClassNode(classname);
			}
		};
	}




}
