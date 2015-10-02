package net.fybertech.meddleapi.tweak;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import net.fybertech.meddle.Meddle;
import net.fybertech.meddle.Meddle.ModContainer;
import net.fybertech.meddle.MeddleMod;
import net.fybertech.meddle.MeddleUtil;
import net.fybertech.meddleapi.transformer.AccessTransformer;
import net.fybertech.meddleapi.transformer.ClientTransformer;
import net.fybertech.meddleapi.transformer.ReobfTransformer;
import net.fybertech.meddleapi.transformer.Transformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

@MeddleMod(id="meddleapi", name="MeddleAPI", version="1.0.4", author="FyberOptic", depends={"dynamicmappings"})
public class APITweaker implements ITweaker
{	
		
	@Override
	public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) 
	{
		/*for (ModContainer mc : Meddle.discoveredModsList) 
		{
			JarFile jar = null;
			try {
				jar = new JarFile(mc.jar);
						
				ZipEntry entry = jar.getEntry("accesstransformer.cfg");
				if (entry != null) 
				{
					BufferedReader reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry)));				
					String line = null;
					
					while ((line = reader.readLine()) != null) {
						// Ignore commented or short lines
						if (line.startsWith("#") || line.length() < 1) continue;
						
						String[] split = line.split(" ", 1);
						if (split.length < 2) continue;
						
						String mode = split[0].toUpperCase();
						String ac = split[1];
												
						if (mode.equals("F")) accessTransformerFields.add(ac);
						else if (mode.equals("M")) accessTransformerMethods.add(ac);						
					}
				}				
			
				jar.close();
			} 
			catch (IOException e) {}
			finally { try { if (jar != null) jar.close(); } catch (IOException e) {} }
		}*/
	}

	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader) 
	{	
		classLoader.registerTransformer(AccessTransformer.class.getName());
		classLoader.registerTransformer(ReobfTransformer.class.getName());		
		classLoader.registerTransformer(Transformer.class.getName());
		if (MeddleUtil.isClientJar()) classLoader.registerTransformer(ClientTransformer.class.getName());		
	}

	@Override
	public String getLaunchTarget() {
		return null;
	}

	@Override
	public String[] getLaunchArguments() {
		return new String[0];
	}

}
