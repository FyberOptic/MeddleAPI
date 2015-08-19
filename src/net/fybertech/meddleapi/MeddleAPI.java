package net.fybertech.meddleapi;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import net.fybertech.dynamicmappings.DynamicMappings;
import net.fybertech.meddle.Meddle;
import net.fybertech.meddle.MeddleUtil;
import net.fybertech.meddle.Meddle.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MeddleAPI
{

	public static final Logger LOGGER = LogManager.getLogger("MeddleAPI");
	public static List<Object> apiMods = new ArrayList<Object>();

	public static CommonProxy proxy;
	
	public static Object mainObject = null;


	public static String getVersion()
	{
		return "1.0";
	}


	public static void preInit(Object obj)
	{
		LOGGER.info("[MeddleAPI] PreInit Phase");
		mainObject = obj;
		
		proxy = (CommonProxy)createProxyInstance("net.fybertech.meddleapi.CommonProxy",  "net.fybertech.meddleapi.ClientProxy");
		
		
		for (ModContainer meddleMod : Meddle.discoveredModsList) 
		{
			Manifest manifest = null;
			try {
				manifest = new JarFile(meddleMod.jar).getManifest();
			} catch (IOException e) {}
			if (manifest == null) continue;

			Attributes attr = manifest.getMainAttributes();
			if (attr == null) continue;

			String apiModsAttr = attr.getValue("MeddleAPI-Mods");
			if (apiModsAttr == null || apiModsAttr.length() < 1) continue;
			
			String[] mods = apiModsAttr.split(" ");
			for (String className : mods) 
			{
				try {
					LOGGER.info("[MeddleAPI] Initializing mod class " + className);
					Object c = Class.forName(className, true, MeddleAPI.class.getClassLoader()).newInstance();					
					apiMods.add(c);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			
		}
		
	}

	public static void init()
	{
		LOGGER.info("[MeddleAPI] Init Phase");

		for (Object modClass : apiMods) {		
			try {
				LOGGER.info("[MeddleAPI] Init() - " + modClass.getClass().getName());
				Method init = modClass.getClass().getMethod("init");
				init.invoke(modClass);
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}

		proxy.refreshResources();
	}



	private static Method registerItemMethod = null;

	public static void registerItem(int itemID, String itemName, Item item)
	{
		if (registerItemMethod == null)
		{
			String itemClassName = DynamicMappings.getClassMapping("net.minecraft.item.Item");

			Class itemClass = null;
			try {
				itemClass = Class.forName(itemClassName);
			}
			catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

			Method[] methods = itemClass.getDeclaredMethods();
			for (Method method : methods) {
				Class[] types = method.getParameterTypes();
				if (types.length != 3) continue;
				if (types[0] != int.class) continue;
				if (types[1] != String.class) continue;
				if (types[2] != itemClass) continue;
				method.setAccessible(true);
				registerItemMethod = method;
			}
		}

		try {
			registerItemMethod.invoke(null, itemID, itemName, item);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	// Creates an instance of a class based on whether we're a client or server jar 
	public static Object createProxyInstance(String serverClass, String clientClass) 
	{	
		Class<? extends Object> c = null;
		
		if (MeddleUtil.isClientJar()) {
			try {
				c = Class.forName(clientClass);
			}
			catch (ClassNotFoundException e) {
				LOGGER.error("[MeddleAPI] Unable to find client proxy class " + clientClass);
				return null;
			}			
		}
		else {
			try {
				c = Class.forName(serverClass);
			}
			catch (ClassNotFoundException e) {
				LOGGER.error("[MeddleAPI] Unable to find server proxy class " + serverClass);
				return null;
			}		
		}		
		
		try {
			Object o = c.getConstructor().newInstance();
			return o;
		}
		catch (Exception e) { 
			LOGGER.error("[MeddleAPI] An error occured while initializing proxy class " + c.getName());			
			e.printStackTrace();
		}
		
		
		
		return null;
	}

}
