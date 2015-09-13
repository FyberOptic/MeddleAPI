package net.fybertech.meddleapi;

import net.fybertech.meddleapi.MeddleClient.ICustomBlockRenderer;
import net.fybertech.meddleapi.MeddleClient.IKeyBindingState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
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

	public static void displayGuiHook(EntityPlayerSP player, IInteractionObject iiobject)
	{
		System.out.println("displayGui: " + iiobject.getGuiID());
		
		MeddleClient.IDisplayGui handler = MeddleClient.guiHandlers.get(iiobject.getGuiID());
		if (handler != null) handler.onOpenGui(player, iiobject.getGuiID(), iiobject.getDisplayName(), 0);
	}
	
	
	public static boolean handleOpenWindowHook(EntityPlayerSP player, S2DPacketOpenWindow packet)
	{
		System.out.println("openWindowHook: " + packet.getGuiId() + " " + packet.getSlotCount());	
		
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
