package net.fybertech.meddleapi;

import net.minecraft.server.MinecraftServer;

public class CommonProxy 
{
	public void refreshResources() {}

	public MinecraftServer getServer(Object mainObject) 
	{	
		if (mainObject instanceof MinecraftServer) return (MinecraftServer)mainObject;
		else return null;
	}
}
