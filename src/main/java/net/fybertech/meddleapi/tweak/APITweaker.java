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

@MeddleMod(id="meddleapi", name="MeddleAPI", version="1.0.6", author="FyberOptic", depends={"dynamicmappings"})
public class APITweaker implements ITweaker
{	
		
	@Override
	public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) 
	{		
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
