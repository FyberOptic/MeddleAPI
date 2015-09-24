package net.fybertech.meddleapi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class ConfigFile 
{
	private File configFile = null;
	boolean changed = false;
	

	public Map<String, Map<String, String>> categoryMaps = new HashMap<String, Map<String, String>>();
	
	@SuppressWarnings("rawtypes")
	public Set<ConfigKey> configData = new HashSet<ConfigKey>();
	

	private List<String> readTextFile(File file)
	{
		List<String> output = new ArrayList<String>();
		
		Scanner reader = null;
		try { reader = new Scanner(file); }
		catch (IOException e) { e.printStackTrace(); }		
		if (reader == null) return null;

		while (reader.hasNextLine()) output.add(reader.nextLine());
		reader.close();
		
		return output;
	}


	public ConfigFile()
	{		
	}
	
	public ConfigFile(File f)
	{
		configFile = f;
	}
	
	public void load()
	{
		if (configFile == null || !configFile.exists()) return;
		
		List<String> lines = readTextFile(configFile);
		if (lines == null) return;
		
		categoryMaps.clear();
		
		Map<String, String> currentCategory = null;				
		
		for (String line : lines)
		{
			line = line.trim();
			if (line.startsWith("#") || line.length() < 1) continue;
			
			if (line.startsWith("[") && line.endsWith("]"))
			{
				String categoryName = line.substring(1, line.length() - 1);	
				if (categoryMaps.containsKey(categoryName)) {
					currentCategory = categoryMaps.get(categoryName);
				}
				else {
					currentCategory = new HashMap<String, String>();
					categoryMaps.put(categoryName, currentCategory);
				}			
			}
			else if (line.contains("="))
			{
				if (currentCategory == null)
				{
					currentCategory = new HashMap<String, String>();					
					categoryMaps.put("general", currentCategory);
				}
				
				String key = line.substring(0, line.indexOf('=') - 1).trim().toLowerCase();
				String rest = line.substring(line.indexOf('=') + 1).trim();
				currentCategory.put(key,  rest);
			}
		}
		
	}
	
	
	public void save()
	{
		try {
			PrintWriter pw = new PrintWriter(configFile);
			
			for (String catName : categoryMaps.keySet()) {
				pw.println("[" + catName + "]");
				
				for (ConfigKey key : configData) {
					if (!key.categoryName.equals(catName)) continue;					
					
					if (key.description != null) {
						pw.println("  # " + key.description.replace("\n", "\n  # "));
						if (key.defaultValue != null) {
							pw.println("  #\n  # Default: " + key.defaultValue.toString());
						}
					}
					pw.println("  " + key.keyName + " = " + key.value.toString());
					if (key.description != null) pw.println();
				}
			}	
			
			pw.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}
	

	public boolean hasChanged() {
		return changed;
	}


	protected Map<String, String> getCategory(String categoryName)
	{
		if (categoryName == null) categoryName = "general";
		
		if (categoryMaps.containsKey(categoryName)) return categoryMaps.get(categoryName);
		else {
			changed = true;
			Map<String, String> category = new HashMap<String, String>();
			categoryMaps.put(categoryName, category);
			return category;
		}
	}
	
	
	public int stringToInt(String s)
	{
		int output = 0;
		
		try {
			output = Integer.parseInt(s);
		}
		catch (NumberFormatException e) {}
		
		return output;
	}
	
	
	public <T> T get(ConfigKey<T> key)
	{
		if (configData.contains(key)) return key.value;
		
		Map<String, String> category = getCategory(key.categoryName);
		T output;
		
		if (category.containsKey(key.keyName.toLowerCase())) {
			String s = category.get(key.keyName.toLowerCase());
			if (key.defaultValue instanceof Integer) key.setValue((T)(Integer)stringToInt(s));
			else key.setValue((T)s);
			configData.add(key);
			output = key.value;
		}
		else {
			changed = true;
			configData.add(key);
			output = key.value;
		}		
		
		return output;
	}
	
	
	
	
	public static ConfigKey<String> key(String keyName)
	{
		return new ConfigKey<String>("general", keyName, "", null);
	}	
	
	public static <T> ConfigKey<T> key(String keyName, T defaultValue)
	{
		return new ConfigKey<T>("general", keyName, defaultValue, null);
	}
	
	public static <T> ConfigKey<T> key(String categoryName, String keyName, T defaultValue)
	{
		return new ConfigKey<T>(categoryName, keyName, defaultValue, null);
	}
	
	public static <T> ConfigKey<T> key(String categoryName, String keyName, T defaultValue, String description)
	{
		return new ConfigKey<T>(categoryName, keyName, defaultValue, description);
	}
	
	
	public static class ConfigKey<T>
	{
		public final String categoryName;
		public final String keyName;
		public final T defaultValue;
		public final String description;
		
		public T value;
		
		
		public ConfigKey(String categoryName, String keyName, T defaultValue, String description)
		{
			this.categoryName = (categoryName != null ? categoryName : "general");
			this.keyName = keyName;
			this.defaultValue = defaultValue;
			this.description = description;
			
			this.value = defaultValue;
		}
		
		public void setValue(T value)
		{
			this.value = value;
		}
		
		public int hashCode() 
		{ 
			int hash = 1;
			hash = hash * 31 + (categoryName != null ? categoryName.hashCode() : 0);
			hash = hash * 31 + (keyName != null ? keyName.hashCode() : 0);
			return hash;
		}
	}
	
}

