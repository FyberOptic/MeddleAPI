package net.fybertech.meddleapi;

import java.lang.reflect.Field;

import net.fybertech.dynamicmappings.DynamicMappings;
import net.fybertech.meddle.Meddle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.server.MinecraftServer;

public class ClientProxy extends CommonProxy
{
	
	@Override
	public void refreshResources() {
		super.refreshResources();
		
		Minecraft.getMinecraft().refreshResources();
	}
	
	
	@Override
	public MinecraftServer getServer(Object mainObject) 
	{	
		if (mainObject instanceof Minecraft) return ((Minecraft)mainObject).getIntegratedServer();
		else return super.getServer(mainObject);
	}
	
}
