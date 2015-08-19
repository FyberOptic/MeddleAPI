package net.fybertech.meddleapi;

import java.lang.reflect.Field;

import net.fybertech.dynamicmappings.DynamicMappings;
import net.fybertech.meddle.Meddle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;

public class ClientProxy extends CommonProxy
{
	
	@Override
	public void refreshResources() {
		super.refreshResources();
		
		Minecraft.getMinecraft().refreshResources();
	}
	
	
	static FontRenderer fontRenderer = null;
	static String mappingsVersion = null;
	
	public static void drawMainMenuBranding(GuiScreen gui)
	{				
		if (fontRenderer == null) {
			String fontRendererField = DynamicMappings.getFieldMapping("net/minecraft/client/gui/GuiScreen fontRendererObj Lnet/minecraft/client/gui/FontRenderer;");
			if (fontRendererField == null) return;
			
			Field f = null;
			try {
				f = GuiScreen.class.getDeclaredField(fontRendererField.split(" ")[1]);				
				f.setAccessible(true);
				fontRenderer = (FontRenderer)f.get(gui);
			} catch (Exception e) {}
			
			if (fontRenderer == null) return;
		}
			
		if (mappingsVersion == null) {
			Meddle.ModContainer mc = Meddle.loadedModsList.get("dynamicmappings");
			if (mc != null) mappingsVersion = mc.meta.version();
			else mappingsVersion = "n/a";
		}
		
		int modCount;
		String modOrMods;		
		
		gui.drawString(fontRenderer, "Meddle " + Meddle.getVersion(),  2,  gui.height - 60,  0xFFFFFF);		
		modCount = Meddle.loadedModsList.size();
		modOrMods = modCount == 1 ? " mod" : " mods";
		gui.drawString(fontRenderer, "  " + modCount + modOrMods + " loaded", 2,  gui.height - 50,  0xAAAAAA);
		
		gui.drawString(fontRenderer, "MeddleAPI " + MeddleAPI.getVersion(),  2,  gui.height - 40,  0xFFFFFF);		
		modCount = MeddleAPI.apiMods.size();
		modOrMods = modCount == 1 ? " mod" : " mods";		
		gui.drawString(fontRenderer, "  " + modCount + modOrMods + " loaded", 2, gui.height - 30, 0xAAAAAA);
		
		gui.drawString(fontRenderer, "DynamicMappings " + mappingsVersion, 2, gui.height - 20, 0xFFFFFF);	
	}
}
