package net.fybertech.meddleapi;

import net.fybertech.meddle.Meddle;
import net.fybertech.meddleapi.MeddleClient.ICustomBlockRenderer;
import net.fybertech.meddleapi.MeddleClient.IKeyBindingState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.util.BlockPos;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.IInteractionObject;

public class MeddleClientHooks {

	// Outputs some debug info from some of the hooks
	static boolean debug = false;
	
	// Cached value of the DynamicMappings version
	static String mappingsVersion = null;
	
	
	
	public static void drawMainMenuBranding(GuiScreen gui)
	{		
		FontRenderer fontRenderer = gui.fontRendererObj;
			
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
	
	
	public static void initMainMenuHook(GuiMainMenu gui)
	{
		//System.out.println("INIT GUI");
		
		int x = gui.width / 2 + 104;
		int y = gui.height / 4 + 48;
		gui.buttonList.add(new GuiButton(25, x, y + 72 + 12, 20, 20, "M"));
	}
	
	
	public static boolean actionPerformedMainMenuHook(GuiMainMenu gui, GuiButton button)
	{
		if (button.id == 25) {			
			Minecraft.getMinecraft().displayGuiScreen(new GuiMods(gui));			
			return true;
		}
		else return false;
	}
	
	
	public static void displayGuiHook(EntityPlayerSP player, IInteractionObject iiobject)
	{
		if (debug) System.out.println("displayGui: " + iiobject.getGuiID());
		
		MeddleClient.IDisplayGui handler = MeddleClient.guiHandlers.get(iiobject.getGuiID());
		if (handler != null) handler.onOpenGui(player, iiobject.getGuiID(), iiobject.getDisplayName(), 0);
	}
	
	
	public static boolean handleOpenWindowHook(EntityPlayerSP player, S2DPacketOpenWindow packet)
	{
		if (debug) System.out.println("openWindowHook: " + packet.getGuiId() + " " + packet.getSlotCount());	
		
		MeddleClient.IDisplayGui handler = MeddleClient.guiHandlers.get(packet.getGuiId());
		if (handler != null) 
		{
			handler.onOpenGui(player, packet.getGuiId(), packet.getWindowTitle(), packet.getSlotCount());
			player.openContainer.windowId = packet.getWindowId();
			return true;
		}		
		else return false;
	}
	
	
	public static void setKeyBindStateHook(int keyCode, boolean pressed, KeyBinding keyBinding)
	{
		for (IKeyBindingState handler : MeddleClient.keyBindStateHandlers) {
			handler.onsetKeyBindState(keyCode,  pressed,  keyBinding);
		}
	}
	
	
	public static boolean renderBlockHook(IBlockState state, BlockPos pos, IBlockAccess blockAccess, WorldRenderer renderer)
	{
		if (state == null || state.getBlock() == null) return false;
		
		//int renderID = state.getBlock().getRenderType();
		ICustomBlockRenderer customRenderer = MeddleClient.customBlockRenderers.get(state.getBlock());
		
		if (customRenderer == null) return false;
		else return customRenderer.renderBlock(state, pos, blockAccess, renderer);
	}
	
}
