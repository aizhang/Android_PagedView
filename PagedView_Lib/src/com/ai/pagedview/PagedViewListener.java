package com.ai.pagedview;

/**
 * 
 * @author Isaiah Cheung
 *
 */
public interface PagedViewListener {
	public void onScrollToPage(int fromPage, int toPage);

	public void onSetToPage(int fromPage, int toPage);

}
