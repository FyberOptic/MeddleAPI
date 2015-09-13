package net.fybertech.meddleapi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.BlockPos;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.IInteractionObject;

public class MeddleClient 
{
	public static Map<String, IDisplayGui> guiHandlers = new HashMap<>();
	public static Set<IKeyBindingState> keyBindStateHandlers = new HashSet<>();
	public static Map<Block, ICustomBlockRenderer> customBlockRenderers = new HashMap<>();
	
	public static void registerGuiHandler(IDisplayGui displayGui) 
	{
		for (String handler : displayGui.getHandledGuiIDs()) {
			guiHandlers.put(handler,  displayGui);
		}
	}
	
	public static void registerKeyBindStateHandler(IKeyBindingState handler)
	{
		keyBindStateHandlers.add(handler);
	}
	
	public static void registerCustomBlockRenderer(Block block, ICustomBlockRenderer renderer)
	{
		customBlockRenderers.put(block, renderer);		
	}
	
	
	public static interface ICustomBlockRenderer
	{
		public boolean renderBlock(IBlockState state, BlockPos pos, IBlockAccess blockAccess, WorldRenderer renderer);
	}
	
	public static interface IDisplayGui 
	{		
		//public void onOpenGui(EntityPlayerSP player, IInteractionObject guiObject);
		
		public List<String> getHandledGuiIDs();

		public void onOpenGui(EntityPlayerSP player, String guiID, IChatComponent displayName, int i);
	}
	
	public static interface IKeyBindingState
	{
		public void onsetKeyBindState(int keyCode, boolean pressed, KeyBinding keyBinding);
	}

}
