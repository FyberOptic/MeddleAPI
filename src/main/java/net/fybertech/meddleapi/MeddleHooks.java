package net.fybertech.meddleapi;

import java.lang.reflect.Field;
import java.util.List;

import net.fybertech.dynamicmappings.DynamicMappings;
import net.fybertech.meddle.Meddle;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.item.ItemStack;

public class MeddleHooks 
{
	
	/**
	 * Forcefully sends the crafting result to clients.
	 * 
	 * Allows for server-side crafting recipes, otherwise the client doesn't
	 * see the result item (but can still pull it from the slot).
	 */
	public static void containerWorkbenchHook(Container c)
	{		
		for (ICrafting ic : c.crafters) {
			ic.sendSlotContents(c, 0, c.inventorySlots.get(0).getStack());
		}		
	}
	
}
