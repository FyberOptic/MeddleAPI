package net.fybertech.meddleapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.fybertech.dynamicmappings.DynamicMappings;
import net.fybertech.meddle.Meddle;
import net.fybertech.meddle.Meddle;
import net.fybertech.meddle.MeddleUtil;
import net.fybertech.meddle.Meddle.ModContainer;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandManager;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.command.CommandHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Changelog
// v1.0.1
// - Added ability to get MinecraftServer instance
// - Added the ability to register ICommand handlers.
//   -  Handlers are automatically re-registered when client restarts integrated server
// 
// v1.0.2
// - Added access transformer
// - Added some early GUI hooks
// - Added a basic config file system
// - Added custom block renderer
// 
// v1.0.3
// - Added mappings scan for mods to confirm they exist before loading
// - Fixed bug in ConfigFile, keys with capitalization weren't being found.
//
// v1.0.4
// - Removed some debug output that got left in
// - Added proper access transformer system
// -


public class MeddleAPI
{

	public static final Logger LOGGER = LogManager.getLogger("MeddleAPI");
	public static List<Object> apiMods = new ArrayList<Object>();

	// Only needed for clients, they're registered immediately on servers
	public static List<ICommand> delayedICommands = new ArrayList<ICommand>();
	
	public static CommonProxy proxy = (CommonProxy)createProxyInstance("net.fybertech.meddleapi.CommonProxy",  "net.fybertech.meddleapi.ClientProxy");
	
	public static Object mainObject = null;


	public static String getVersion()
	{
		return "1.0.4-alpha";
	}


	
	/**
	 * Compares a list of mappings in the specified file to the available mappings
	 * and returns a list of the ones not available.  Takes client and server-side 
	 * availability into consideration.
	 * 
	 * The format of a generatedmappings.cfg file is:  [C|F|M] [1|2|3] MAPPING
	 * 
	 * The first parameter is the type of mapping: 
	 * C = Class, F = Field, M = method
	 * 
	 * The second parameter is the Minecraft "side":
	 * 1 = client, 2 = server, 3 = either
	 * 
	 * The mapping is the exact line of the mapping from DynamicMappings.
	 * 
	 * @param stream The stream of a generatedmappings.cfg-formatted file.
	 * @return The list of mappings that the file specifies which aren't present.
	 * @throws IOException
	 */
	private static List<String> findMissingMappings(InputStream stream) throws IOException
	{
		List<String> missingMappings = new ArrayList<>();	
		boolean isClient = MeddleUtil.isClientJar();			
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));				
		String line = null;
		
		while ((line = reader.readLine()) != null) {
			// Ignore commented or short lines
			if (line.startsWith("#") || line.length() < 1) continue;
			
			// Format: [C|F|M] [1|2|3] MAPPING
			String[] split = line.split(" ", 3);
			if (split.length != 3) continue;
			
			// 1 == client, 2 == server, 3 == either
			int side = 0;
			try { side = Integer.parseInt(split[1]); }
			catch (NumberFormatException e) { continue; }
			if (side < 1 || side > 3) continue;
			
			// Skip if we're the client and it's a server mapping
			if (isClient && side == 2) continue;
			// Skip if we're the server and it's a client mapping
			if (!isClient && side == 1) continue;
			
			boolean hasMapping = false;
			String mapping = split[2];
			
			char type = split[0].toUpperCase().charAt(0);
			switch (type) {
				case 'C': if (DynamicMappings.classMappings.containsKey(mapping)) hasMapping = true; break;
				case 'F': if (DynamicMappings.fieldMappings.containsKey(mapping)) hasMapping = true; break;
				case 'M': if (DynamicMappings.methodMappings.containsKey(mapping)) hasMapping = true; break;
			}
			
			if (!hasMapping) missingMappings.add(mapping);
		}		
		reader.close();
		
		return missingMappings;
	}
	
	
	/**
	 * Locates a generatedmappings.cfg in the specified mod jar, and if present, scans it
	 * to confirm all listed mappings are present.  
	 * 
	 * @see findMissingMappings(InputStream stream)
	 * @param jarFile The mod jar to analyze for mappings.
	 * @return True if all mappings exist or if no generatedmappings.cfg is present.
	 */
	private static boolean hasRequiredMappings(JarFile jarFile)
	{
		ZipEntry entry = jarFile.getEntry("requiredmappings.cfg");
		if (entry == null) return true;		
		
		String jarName = jarFile.getName().replace("\\", "/");
		if (jarName.contains("/")) jarName = jarName.substring(jarName.lastIndexOf('/') + 1);
		
		List<String> missingMappings;
		try {
			missingMappings = findMissingMappings(jarFile.getInputStream(entry));
		} catch (IOException e) {
			LOGGER.error("[MeddleAPI] Error reading requiredmappings.cfg from " + jarName);
			return false;		
		}
		
		if (missingMappings.size() > 0) {
			LOGGER.error("[MeddleAPI] " + jarName + " has missing mappings, aborting mod initialization");
			return false;		
		}
		
		LOGGER.info("[MeddleAPI] All mappings present for " + jarName);
		
		return true;
	}


	/**
	 *
	 * @param obj
	 */
	public static void preInit(Object obj)
	{
		LOGGER.info("[MeddleAPI] PreInit Phase");
		mainObject = obj;
		
		
		for (ModContainer meddleMod : Meddle.discoveredModsList) 
		{
			Manifest manifest = null;
			JarFile jarFile = null;
			
			try {
				jarFile = new JarFile(meddleMod.jar);
			} catch (IOException e) { continue; }
						
			try {
				manifest = jarFile.getManifest();
			} catch (IOException e) {}
			if (manifest == null) continue;

			Attributes attr = manifest.getMainAttributes();
			if (attr == null) continue;

			String apiModsAttr = attr.getValue("MeddleAPI-Mods");
			if (apiModsAttr == null || apiModsAttr.length() < 1) continue;
			
			if (!hasRequiredMappings(jarFile)) continue;
			
			try {
				if (jarFile != null) jarFile.close();
			} catch (IOException e) {}
			
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


	/**
	 *
	 */
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

	// TODO - Switch to using method directly now that we have access transformer
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

	
	
	public static void registerBlock(int blockID, String blockName, Block block)
	{
		Block.registerBlock(blockID, blockName, block);		
		
		Iterator<IBlockState> it = block.getBlockState().getValidStates().iterator();
		while (it.hasNext()) {
			IBlockState ibs = it.next();
			int var23 = Block.blockRegistry.getIDForObject(block) << 4 | block.getMetaFromState(ibs);
            Block.BLOCK_STATE_IDS.put(ibs, var23); 
		}		
		
		Item.registerItemBlock(block);
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
	
	
	public static ICommandManager getICommandManager()
	{
		return MeddleAPI.getServer().getCommandManager();
	}
	
	
	public static MinecraftServer getServer()
	{
		return proxy.getServer(mainObject);
	}
	
	
	
	public static void registerCommandHandler(ICommand cmd)
	{
		if (MeddleUtil.isClientJar()) delayedICommands.add(cmd);
		else {
			((CommandHandler) getServer().getCommandManager()).registerCommand(cmd);
		}
	}
	
	public static void onServerRunHook(MinecraftServer server)
	{	
		if (MeddleUtil.isClientJar()) {			
			CommandHandler cmdHandler = (CommandHandler) server.getCommandManager();
			for (ICommand cmd : delayedICommands)
			{
				cmdHandler.registerCommand(cmd);
			}
		}
	}

}
