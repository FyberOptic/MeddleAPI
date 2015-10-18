package net.fybertech.meddleapi;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class GuiMods extends GuiScreen {

	private GuiScreen parentScreen;
	
	public GuiMods(GuiScreen parent)
	{
		this.parentScreen = parent;
	}
	
	
	@Override
	public void initGui() {	
		super.initGui();
		
		int x = this.width / 2 - 100;
		int y = this.height - 30;
		this.buttonList.add(new GuiButton(0, x, y, 200, 20, "Done"));
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) 
	{		
		this.drawDefaultBackground();		
		drawCenteredString(this.fontRendererObj, "Meddle Config", this.width / 2, 10, 16777215);
		drawCenteredString(this.fontRendererObj, "(TODO)", this.width / 2, this.height / 2, 16777215);
		
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
	
	@Override
	protected void actionPerformed(GuiButton param0) 
	{
		this.mc.displayGuiScreen(this.parentScreen);
	}
	
	
	
}
