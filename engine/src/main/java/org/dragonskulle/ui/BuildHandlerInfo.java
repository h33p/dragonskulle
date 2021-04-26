package org.dragonskulle.ui;

import org.dragonskulle.ui.UIManager.IUIBuildHandler;

import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
public class BuildHandlerInfo {
	@Getter private IUIBuildHandler mHandler;
	@Getter private float mXOffset;
	
	public BuildHandlerInfo(IUIBuildHandler handler) { 
		mHandler = handler;
		mXOffset = 0;
	}
	
	public BuildHandlerInfo(IUIBuildHandler handler, float xOffset) { 
		mHandler = handler;
		mXOffset = xOffset;
	}
}
