/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.android.contacts.common.widget;

import java.security.InvalidParameterException;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.LinearInterpolator;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.view.animation.AlphaAnimation;

import com.android.contacts.common.R;


/**
 * The BladeView provide user to location a specific item in list by clicking
 * the category instead of scrolling the list.
 * 
 * In order to use this control, your adapter must implement the Indexer interface,
 * then if you want the thumb synchronize when the list scrolling, you must implement
 * the onScroll function to calculate the current section and set it to BladeView, so
 * the BladeView will redraw itself to perform synchronization.
 * 
 * As an blade view, the full sections and replaced section should set before drawing 
 * process start, you can either set it in xml file as attributes set or call functions,
 * we don't judge whether it is valid, so user need to pay attention, same with section 
 * baselines and indicator tops. 
 * 
*/

public class BladeView extends View {
    private static final String TAG = "BladeView";
    private static final boolean DBG = false;
    
    private static final int DEFAULT_SECTION_ENABLE_COLOR = Color.BLACK;
    private static final int DEFAULT_SECTION_DISABLE_COLOR = Color.argb( 0xFF, 0xA5, 0xA5, 0xA5 );
    private static final int DEFAULT_PROMPT_ENABLE_COLOR = Color.WHITE;
    private static final int DEFAULT_PROMPT_DISABLE_COLOR = Color.argb( 0xFF, 0xA5, 0xA5, 0xA5 );
    
    private static final int PROMPT_ANIM_DURATION = 1000;	//modify by zengxx 
    private static final int PROMPT_HIDE_ANGLE = -90;
    private static final int PROMPT_SHOW_ANGLE = 0;
    
    /* Sections include the absent ones. */
    private String[] mFullSections;
    
    /* Contains full sections, but some of its elements are replaced with dashes to save space. */
    private String[] mReplacedSections;
    private int mDevisionLineCount = 0;
    private int mFullSectionLen;
    
    /* The baselines of each sections and top of indicators. */
    private int[] mSectionBaselines;
    private int[] mIndicatorTops;
    
    /* The real sections got from list. */
    private Object[] mListSections;
    private int mListSectionLen;
    
    /* The related list view and adapter. */
    private AbsListView mList;
    private BaseAdapter mListAdapter;
    /* When it is an header-footer list, represents the count of headers, 0 in normal list. */
    private int mListOffset;
    private SectionIndexer mSectionIndexer;

    /* A boolean array used to indicate whether the section is in the list, true if absent. */
    private boolean[] mIsAbsent;
    
    /* Use enable color and disable color to distinguish sections present and absent. */
    private Paint mEnableSectionPaint;
    private int mEnablePaintColor;

    private Paint mDisableSectionPaint;
    private int mDisablePaintColor;
    
    private int mEnablePromptColor;
    private int mDisablePromptColor;
    
    /* The prompt window and the text view showing in the window. */ 
    private PopupWindow mPromptWindow;
    private TextView mPromptText;
    private int mPromptHorzOffset;
    private int mPromptVertOffset;
    private Handler mHandler;
    
    
	/*begin add by zengxx 2012-02-14 */
    private AlphaAnimation mEnterAlphaAnim;
    private AlphaAnimation mExitAlphaAnim;
	/*end add by zengxx 2012-02-14*/
    private int mAnimationDuration;    
    
    /* Indicator drawable overlay the current section to prompt user whether he/she is. */
    private Drawable mIndicatorDrawable;
    
    /* The width and height of the indicator. */
    private int mIndicatorWidth;
    private int mIndicatorHeight;
    private Rect mIndicatorRect = new Rect();
    
    /* 
     * In order to optimize the drawing time, we only draw the dirty area of the view,
     * mRedrawRect restore information of the area.
     */
    private Rect mRedrawRect = new Rect();
    
    /* Font size of the sections. */
    private int mSectionFontSize; 
    
    /* The current selected position in full sections. */
    private int mCurrentSection;
    
    /* The position of the previous selected section in full sections. */
    private int mOldSection;
    
    /* Record the previous motion position to reduce unnecessary list operation. */
    private int mLastMotionY;
    
    /* The section should be resurrected when user release his/her finger.*/
    private int mResurrectSection = -1;
    
    /* When handling touch event, block the setCurrentSection message sent by client.*/
    private boolean mBlockSetCurrent = false;
    
    public BladeView(Context context) {
        this(context, null);
    }   
    
    public BladeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public BladeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);   
        
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BladeView);
        final Resources resources = context.getResources();
        
        /*Get the rotate animation duration from attributes set. */
        mAnimationDuration = a.getInt(R.styleable.BladeView_promptAnimationDuration, PROMPT_ANIM_DURATION);

        mSectionFontSize = a.getDimensionPixelSize(R.styleable.BladeView_sectionFontSize, 
                resources.getDimensionPixelSize(R.dimen.blade_font_size));
        
        mEnablePaintColor = a.getColor(R.styleable.BladeView_enableSectionColor,
        		DEFAULT_SECTION_ENABLE_COLOR);
        mDisablePaintColor = a.getColor(R.styleable.BladeView_disableSectionColor,
        		DEFAULT_SECTION_DISABLE_COLOR);
        
        mEnablePromptColor = a.getColor(R.styleable.BladeView_enablePromptColor,
        		DEFAULT_PROMPT_ENABLE_COLOR);
        mDisablePromptColor = a.getColor(R.styleable.BladeView_disablePromptColor,
        		DEFAULT_PROMPT_DISABLE_COLOR);
        
        /* Get the indicator drawable and its size from attributes set. */
        final Drawable d = a.getDrawable(R.styleable.BladeView_bladeIndicator);
        if (d != null) {
            mIndicatorDrawable = d;
        } else {
            mIndicatorDrawable = resources.getDrawable(R.drawable.blade_indicator_normal);
        }
        
        mIndicatorWidth = a.getDimensionPixelSize(R.styleable.BladeView_bladeIndicatorWidth, 
                resources.getDimensionPixelSize(R.dimen.blade_indicator_width));
        mIndicatorHeight = a.getDimensionPixelSize(R.styleable.BladeView_bladeIndicatorHeight,
                resources.getDimensionPixelSize(R.dimen.blade_indicator_height));
        
        /* Get the offset of the prompt window. */
        mPromptHorzOffset = a.getDimensionPixelSize(R.styleable.BladeView_promptHorzOffset, 
                resources.getDimensionPixelSize(R.dimen.blade_prompt_horz_offset));
        mPromptVertOffset = a.getDimensionPixelSize(R.styleable.BladeView_promptVertOffset, 
                resources.getDimensionPixelSize(R.dimen.blade_prompt_vert_offset));
        
        /* Get full sections and replaced sections from attributes set. */
        int fullSectionId = a.getResourceId(R.styleable.BladeView_fullSectionsId, 0);
        int replacedSectionId = a.getResourceId(R.styleable.BladeView_replacedSectionsId, 0); 
        if (fullSectionId == 0 || replacedSectionId == 0) {
            throw new Resources.NotFoundException("You have to specify section resources!");
        }

        setSections(fullSectionId, replacedSectionId);
        
        /* Get section baselines from attributes set. */
        int blResId = a.getResourceId(R.styleable.BladeView_sectionBaselinesId, 0);
        if (blResId == 0) {
            throw new Resources.NotFoundException("You have to specify section baselines!");
        }
        setSectionBaseLines(blResId);
        
        /* Get indicator tops from attributes set. */
        int indResId = a.getResourceId(R.styleable.BladeView_indicatorTopsId, 0);
        if (indResId == 0) {
            throw new Resources.NotFoundException("You have to specify indicator tops!");
        }
        setIndicatorTops(indResId);
        
        a.recycle();
        
        init(context);
    }
    
    private void init(Context context) {         
        /* Get array and other attributes from xml. */
        final Resources resources = context.getResources();
        
        /* Define the attributes of the paint used to draw sections. */
        mEnableSectionPaint = new Paint();
        mEnableSectionPaint.setColor(mEnablePaintColor);
        mEnableSectionPaint.setTypeface(Typeface.MONOSPACE);
        mEnableSectionPaint.setAntiAlias(true);
        mEnableSectionPaint.setTextAlign(Paint.Align.CENTER);
        mEnableSectionPaint.setTextSize(mSectionFontSize);
        
        mDisableSectionPaint = new Paint(mEnableSectionPaint);
        mDisableSectionPaint.setColor(mDisablePaintColor);      
        
        /* Inflate prompt window content view resource from xml file. */
        LayoutInflater layoutInflater = (LayoutInflater)context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mPromptText = (TextView) layoutInflater.inflate(R.layout.blade_popup_text, null);
        mPromptText.setFocusable(false);
        
        /* Handler used to post dismiss the prompt window message. */
        mHandler = new Handler();
        
       
		/*begin add by zengxx 2012-02-14 */
        mEnterAlphaAnim = new AlphaAnimation(0.1f,1.0f);
        mEnterAlphaAnim.setDuration(mAnimationDuration);
        
        mExitAlphaAnim = new AlphaAnimation(1.0f,0f);
        mExitAlphaAnim.setDuration(mAnimationDuration);
		/*end add by zengxx 2012-02-14*/
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas); 
        int offsetX = (getMeasuredWidth() - mIndicatorWidth) >> 1;
        Log.v(TAG, "offsetX:" + offsetX);
        
        //Draw the big black background
        Rect bgRect = new Rect(offsetX, 0, 
        		 offsetX + mIndicatorWidth, getMeasuredHeight());
        Paint bgRectPaint = new Paint();
        bgRectPaint.setColor(Color.parseColor("#F5F5F5"));
        canvas.drawRect(bgRect, bgRectPaint);
        
        //Draw the big black background's left line      
        Paint leftLinePaint = new Paint();      
        leftLinePaint.setColor(Color.parseColor("#F5F5F5"));//#386B7A
        leftLinePaint.setStrokeWidth(2);
        canvas.drawLine(1, 0,
        		       1, getMeasuredHeight(), leftLinePaint);
        
      //Draw the devision line
        long perAddHeight = 4;
      //int cellPerHeight = getResources().getInteger(R.integer.cell_per_height);
        int cellPerHeight = mSectionBaselines[11] - mSectionBaselines[10];
	  /**
        for(int j=0;j<mDevisionLineCount;j++){
        	Paint linePaint = new Paint();
            linePaint.setColor(Color.parseColor("#272727"));
            long offsetY = mSectionBaselines[0] + perAddHeight;
            canvas.drawLine(2, offsetY, 1 + mIndicatorWidth, offsetY, linePaint);
            perAddHeight += cellPerHeight;
        }*/
        
        long leftX = offsetX;
        long rightX = offsetX + mIndicatorWidth;
        long topY = mIndicatorTops[mCurrentSection] - mIndicatorHeight*2;
        long bottomY = mIndicatorTops[mCurrentSection] + mIndicatorHeight;
        Log.v(TAG, "mCurrentSection:" + mCurrentSection);
        
        //Draw the small black background,the current section's position
      //  Paint currentRectPaint = new Paint();
     //   currentRectPaint.setColor(Color.parseColor("#F00000"));
     //   canvas.drawRect(leftX, topY, rightX, bottomY, currentRectPaint);
        
        //Draw the blue bound line of the small black rectangle above
       // Paint recLinePaint = new Paint();
       // recLinePaint.setColor(Color.parseColor("#386B7A"));
       // recLinePaint.setStrokeWidth(2);
        //top line
        //canvas.drawLine(offsetX, topY,rightX, topY, recLinePaint);
        //bottom line
       // canvas.drawLine(offsetX, bottomY,rightX, bottomY, recLinePaint);
        //right line
       // canvas.drawLine(rightX, topY,rightX, bottomY, recLinePaint);
        
        int centerWidth = getMeasuredWidth() >> 1;
        
        //Draw all words
         Paint normalWordPaint = new Paint();
        normalWordPaint.setColor(Color.parseColor("#FF000000"));
        normalWordPaint.setTypeface(Typeface.DEFAULT_BOLD);
	 normalWordPaint.setFakeBoldText(true);	
        normalWordPaint.setAntiAlias(true);
        normalWordPaint.setTextAlign(Paint.Align.CENTER);
        normalWordPaint.setTextSize(mSectionFontSize); 
        for (int i = 0; i < mFullSectionLen; i++) {
	     if(mIsAbsent[i]){
		 	normalWordPaint.setColor(DEFAULT_SECTION_DISABLE_COLOR);
	     }else{
	    		 normalWordPaint.setColor(DEFAULT_SECTION_ENABLE_COLOR);
	     }			
            canvas.drawText(mReplacedSections[i], centerWidth,
                        mSectionBaselines[i], normalWordPaint);
        }
        
        
        
        //highlight the current section's word
        /**
        Paint currentWordPaint = new Paint();
        currentWordPaint.setColor(Color.parseColor("#28AAD9"));
        currentWordPaint.setTypeface(Typeface.MONOSPACE);
        currentWordPaint.setAntiAlias(true);
        currentWordPaint.setTextAlign(Paint.Align.CENTER);
        currentWordPaint.setTextSize(mSectionFontSize);
        canvas.drawText(mReplacedSections[mCurrentSection], centerWidth,
        		       mSectionBaselines[mCurrentSection], currentWordPaint);
        		       */
    }
    
    @Override
	public boolean onTouchEvent(MotionEvent ev) {
		/* If there is no list related, use the default touch event handling process. */
		if (mList == null) {
			return super.onTouchEvent(ev);
		}

		final int action = ev.getAction();
		final int x = (int) ev.getX();
		final int y = (int) ev.getY();
		int toSection;

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			/* If the list is in a flinging, stop it. */
			cancelFling();

			if (mListAdapter == null && mList != null) {
				getSectionsFromIndexer();
			}

			/*
			 * Records the old section position and the calculate the current for later use.
			 */
			mBlockSetCurrent = true;
			mLastMotionY = y;
			mOldSection = mCurrentSection; // Record the old section first.

			toSection = calcSectionIndex(x, y);
			mCurrentSection = toSection;

			if (DBG) {
				Log.i(TAG, "onTouchEvent:ACTION_DOWN,x = " + x + ",y = " + y
						+ ",mCurrentSection = " + mCurrentSection);
			}

			setPromptText(toSection);
			showPromptWindow();

			/* Start the rotate animation as enter effect. */
			/*begin add by zengxx 2012-02-14 */
			mPromptText.startAnimation(mEnterAlphaAnim);
			/*end add by zengxx 2012-02-14*/

			/* Move the list to the related position. */
			moveListToSection(mCurrentSection);

			/* Invalidate the invalid rectangle to do self redraw process if needed. */
			redrawIfNeeded();
			break;

		case MotionEvent.ACTION_MOVE:
			/* Judge whether current motion position and the last motion position are in the same slot. */
			if (y == mLastMotionY) {
				return true;
			} else if (Math.abs(y - mLastMotionY) < mIndicatorHeight) {
				for (int i = 1; i < mFullSectionLen; i++) {
					if (y >= mSectionBaselines[i - 1] && y < mSectionBaselines[i]
							&& mLastMotionY >= mSectionBaselines[i - 1] && mLastMotionY < mSectionBaselines[i]) {
						return true;
					}
				}
			}

			mLastMotionY = y;
			mOldSection = mCurrentSection;
			toSection = calcSectionIndex(x, y);
			mCurrentSection = toSection;

			setPromptText(toSection);
			showPromptWindow();

			if (DBG) {
				Log.i(TAG, "onTouchEvent:ACTION_MOVE,x = " + x + ",y = " + y
						+ ",mCurrentSection = " + mCurrentSection);
			}

			/* Move the list to the related position. */
			moveListToSection(mCurrentSection);
			invalidate();
			break;

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			if (DBG) {
				Log.i(TAG, "onTouchEvent:ACTION_UP or CANCEL,x = " + x + ",y = " + y + 
						",mCurrentSection = " + mCurrentSection);
				Log.v(TAG, "mResurrectSection:" + mResurrectSection);
			}
			/* Dismiss the pop up window if it is currently visible. */
			dismissPromptWindow();

			mBlockSetCurrent = false;
			mLastMotionY = -1;
			if (mCurrentSection != mResurrectSection && mResurrectSection != -1) {
				mCurrentSection = mResurrectSection;
				mResurrectSection = -1;
				invalidate();
			}
			break;

		default:
			/* Dismiss the pop up window if it is currently visible. */
			dismissPromptWindow();
			mBlockSetCurrent = false;
			break;
		}

		return true;
	} 

    /**
     * Calculate the current action affect which section, use section baselines
     * to locate, the bound of each section is between its previous section
     * bottom and the baseline of itself. 
     * 
     * @param x The x axis of the action.
     * @param y The y axis of the action.
     * @return The index in the full sections, revise it when the result is out of bounds.
     */
    private int calcSectionIndex(final float x, final float y) {
        if (DBG) {
            Log.i(TAG, "calcSectionIndex: x = " + x + ",y = " + y);
        }        

        for (int i = 1; i < mFullSectionLen; i++) {
            if (y >= mSectionBaselines[i-1] && y < mSectionBaselines[i]) {
                return i;
            }
        }

        if (y < mSectionBaselines[0]) {
            return 0;
        }

        return mFullSectionLen - 1;
    }

    /**
     * Move the list to the start position of the current selected section.
     * 
     * @param fullSection The index in list sections.
     */
    private void moveListToSection(final int fullSection) {
        int listSection = getListSectionIndex(fullSection);

        int position = mSectionIndexer.getPositionForSection(listSection);
        if (DBG) {
            Log.i(TAG, "moveListToSection: fullSection = " + fullSection + 
                    ",listSection = " + listSection + ",position = " + position);
        }        

        /* Add mListOffset for all list view, because it will be 0 if it has no header. */
       ((ListView) mList).setSelectionFromTop(position + mListOffset, 0);        
    }
    
    /**
     * Translate list section position into index in full section.
     * 
     * @param listIndex The list section position(mListSections).
     * @return The index in full section(mOriginSections), if there is
     *      not return -1.
     */
    private int getFullSectionIndex(int listIndex) {
        for (int i = 0; i < mFullSectionLen; i++) {
            if (mFullSections[i].equals(mListSections[listIndex])) {
                return i;
            }
        }

        return -1;
    }
    
    /**
     * Translate full section index(mOriginSections) to position in list section.
     *  
     * @param fullIndex The index in full section.
     * @return The index in list section.
     */
    private int getListSectionIndex(int fullIndex) {
    
	if("#".equals(mFullSections[fullIndex])){
		for (int i = 0; i < mListSectionLen; i++) {
			if(mListSections[i].toString().equals("#")){
				return i;			
			}
		}
	}
	
    	 for (int i = 0; i < mListSectionLen; i++) {
		if(DBG) Log.e("kaka"+TAG,"fullIndex:"+fullIndex+"mListSectionLen:"+mListSectionLen+"mListSections[i]"+mListSections[i] +"i---"+i+"mFullSections[fullIndex]:"+mFullSections[fullIndex]+"---mFullSections[fullIndex].compareToIgnoreCase(mListSections[i].toString()) <= 0--"+(mFullSections[fullIndex].compareToIgnoreCase(mListSections[i].toString()) <= 0));
		if (mListSections[i] != null
                     && mFullSections[fullIndex].compareToIgnoreCase(mListSections[i].toString()) <= 0) {
                 return i;
             }
         }
         return (mListSectionLen - 1);
    }
    
    /**
     * Send an ACTION_CANCEL message to stop list fling.
     */
    private void cancelFling() {
        MotionEvent cancelFling = MotionEvent.obtain(
                0, 0, MotionEvent.ACTION_CANCEL, 0, 0, 0);
        mList.onTouchEvent(cancelFling);
        cancelFling.recycle();
    }
    
    /**
     * Set text content of the prompt text view.
     * 
     * @param section The index of the current section.
     */
    private void setPromptText(final int section) {
        /* Show pop up window to prompt user where he/she is currently. */
        if(TextUtils.isEmpty(mFullSections[section].trim())){
        	mPromptText.setText(getResources().getString(R.string.user_profile_contacts_list_header));
        }else {
        	mPromptText.setText(mFullSections[section]);
		} 	
		// if (!mIsAbsent[section]) {
		// mPromptText.setTextColor(mEnablePromptColor);
		// } else {
		// mPromptText.setTextColor(mDisablePromptColor);
		// }
		mPromptText.setTextColor(Color.parseColor("#32C8FF"));
    }

    /**
     * Redraw parts of the blade view to optimize if needed.
     */
    private void redrawIfNeeded() {
        mRedrawRect.setEmpty();
        /* Optimized, redraw parts of itself only if the thumb changes its position. */
        if (mIndicatorTops[mCurrentSection] != mIndicatorTops[mOldSection]) {
            if (mCurrentSection > mOldSection) {
                mRedrawRect.set(0, mIndicatorTops[mOldSection], getWidth(),
                        mIndicatorTops[mCurrentSection] + mIndicatorHeight);
            } else {
                mRedrawRect.set(0, mIndicatorTops[mCurrentSection], getWidth(),
                        mIndicatorTops[mOldSection] + mIndicatorHeight);
            }
        }
        invalidate(mRedrawRect);
    }
    
    /**
     * Set full sections and replaced sections with string array resource id.
     * 
     * @param fullSectionId The full section string array resource id.
     * @param replacedSectionId The replaced section string array resource id.
     */
    public void setSections(final int fullSectionId, final int replacedSectionId) {
        final Resources resources = getContext().getResources();
        String[] fullSections = resources.getStringArray(fullSectionId);
        String[] replacedSections = resources.getStringArray(replacedSectionId);
        setSections(fullSections, replacedSections);
    }
    
    /**
     * Set full sections and replaced sections with string arrays.
     * 
     * @param fullSectionArray The full section string array.
     * @param replacedSectionArray The replaced section string array.
     */
    public void setSections(final String[] fullSectionArray, final String[] replacedSectionArray) {
        if (fullSectionArray == null || replacedSectionArray == null) {
            throw new InvalidParameterException("Origin sections and replaced section should not be a null pointer!");
        }
        
        if (fullSectionArray.length != replacedSectionArray.length) {
            throw new InvalidParameterException("The length of origin and replaced sections should be equal !");
        }
        
        mFullSections = fullSectionArray;
        mReplacedSections = replacedSectionArray;
        for(int i=0;i<mReplacedSections.length;i++){
        	if(!TextUtils.isEmpty(mReplacedSections[i].trim())){
        		mDevisionLineCount++;
        	}
        }
        mFullSectionLen = mFullSections.length;
        mIsAbsent = new boolean[mFullSectionLen];
        
        setAbsentSections();
        invalidate();
    }
    
    /**
     * Get the length of full sections.
     * 
     * @return The length of full sections.
     */
    public int getFullSectionLength() {
        return mFullSectionLen;
    }
    
    /**
     * Set the section drawing baselines array resource id.
     * 
     * @param blResId The baseline array resource id.
     */
    public void setSectionBaseLines(final int blResId) {
        final Resources resources = getContext().getResources();
        int[] baselines = resources.getIntArray(blResId);
        setSectionBaseLines(baselines);
    }
    
    /**
     * Set the section drawing baselines.
     * 
     * @param baselines The baselines integer array.
     */
    public void setSectionBaseLines(final int[] baselines) {        
        if (baselines == null) {
            throw new InvalidParameterException("Baselines should not be a null pointer.");
        }
        
        if (baselines.length != mFullSections.length) {
            throw new InvalidParameterException(
                    "The length of baselines and full section should be equal!");
        }
        
        mSectionBaselines = baselines;
        invalidate();
    }
    
    /**
     * Set the tops of each indicator with resource id.
     * 
     * @param indResId The tops array resource id.
     */
    public void setIndicatorTops(final int indResId) {
        final Resources resources = getContext().getResources();
        int[] indTops = resources.getIntArray(indResId);
        setIndicatorTops(indTops);
    }
    
    /**
     * Set the tops of each indicator with an integer array.
     * 
     * @param indTops The tops array.
     */
    public void setIndicatorTops(final int[] indTops) {
        if (indTops == null) {
            throw new InvalidParameterException("Indicator tops should not be a null pointer.");
        }
        
        if (indTops.length != mFullSections.length) {
            throw new InvalidParameterException(
                    "The length of indicator tops and full section should be equal!");
        }
        
        mIndicatorTops = indTops;   
        invalidate();
    }
    
    /**
     * Set the indicator drawable to the specified resource.
     * 
     * @param indicator The drawable resource.
     */
    public void setIndicatorDrawable(final Drawable indicator) {
        if (indicator != null) {
            mIndicatorDrawable = indicator;
            invalidate();
        }
    }
    
    /**
     * Get the indicator drawable.
     * 
     * @return The indicator drawable.
     */
    public Drawable getIndicatorDrawable() {
        return mIndicatorDrawable;
    }
    
    /**
     * Set the height of the indicator.
     * 
     * @param height The height of the indicator.
     */
    public void setIndicatorHeight(final int height) {
        mIndicatorHeight = height;
        invalidate();
    }
    
    /**
     * Get the height of the indicator.
     * 
     * @return The height of the indicator.
     */
    public int getIndicatorHeight() {
        return mIndicatorHeight;
    }
    
    /**
     * Get the length of list sections.
     * 
     * @return The length of list sections.
     */
    public int getListSectionLength() {
        return mListSectionLen;
    }
    
    /**
     * Get the current position in full sections.
     * 
     * @return The current section index.
     */
    public int getCurrentSection() {
        return mCurrentSection;
    }
    
    /**
     * Set current index to the specified sectionIndex, this will be
     * called after the related list scrolled, the indicator will also
     * update it position if needed. 
     * 
     * @param listIndex The position in the list section array, it will be 
     *      translated to the index in the full origin section array. 
     */
    public void setCurrentSection(final int listIndex) {
        /* If there isn't any sections, do nothing, just return. */
        if (mListSections == null) {
            return;
        }
        
        int fullIndex = getFullSectionIndex(listIndex);
        if (mCurrentSection == fullIndex) {
            mResurrectSection = mCurrentSection;
            return;
        }

        if (mBlockSetCurrent) {
        	mResurrectSection = fullIndex; 
        	if (mResurrectSection < 0) {
        		mResurrectSection = 0;
            } else if (mResurrectSection > mFullSectionLen - 1) {
            	mResurrectSection = mFullSectionLen - 1;
            }
        } else {        	
            mOldSection = mCurrentSection;
            mCurrentSection = fullIndex; 
            
            if (mCurrentSection < 0) {
                mCurrentSection = 0;
            } else if (mCurrentSection > mFullSectionLen - 1) {
                mCurrentSection = mFullSectionLen - 1;
            }
            
            redrawIfNeeded();
        }
        invalidate();
    }

    /**
     * Set the related list view and get indexer information from it, 
     * every time when you change the data of list, you should call it 
     * to reset the list.
     * 
     * @param listView The related list view.
     */
    public void setList(final AbsListView listView) {
        if (listView != null) {
            mList = listView;
            
            /* Disable fast scroller and hide the vertical scroll bar. */
            mList.setFastScrollEnabled(false);
            mList.setVerticalScrollBarEnabled(false);
            
            /* Get section indexers information form list. */
            getSectionsFromIndexer();
            invalidate();
        } else {
            throw new IllegalArgumentException("Can not set a null list!");
        }
    }
    
    /**
     * Get the related list view.
     * 
     * @return The related list.
     */
    public AbsListView getList() {
        return mList;
    }
     
    /**
     * Get sections and section indexers, then initialize adapter with the list
     * adapter, in the end of the function, the absent array will be updated.
     */
    private void getSectionsFromIndexer() {
        Adapter adapter = mList.getAdapter();
        mSectionIndexer = null;
        if (adapter instanceof HeaderViewListAdapter) {
            mListOffset = ((HeaderViewListAdapter) adapter).getHeadersCount();
            adapter = ((HeaderViewListAdapter) adapter).getWrappedAdapter();
        }

        if (adapter instanceof SectionIndexer) {
            mListAdapter = (BaseAdapter) adapter;
            mSectionIndexer = (SectionIndexer) adapter;
            mListSections = mSectionIndexer.getSections();
            String contactsListHeaderString = getResources().getString(R.string.user_profile_contacts_list_header);
            for(int i=0;i<mListSections.length;i++){
            	if(mListSections[i].toString().equalsIgnoreCase(contactsListHeaderString)){
            		mListSections[i] = " " + contactsListHeaderString;
            	}
            }
            
            mListSectionLen = mListSections.length;
            
            
        } else {
            mListAdapter = (BaseAdapter) adapter;
            mListSections = new String[] { "" };
            mListSectionLen = 0;
        }
        if (DBG) {
            Log.i(TAG, "+++++ = " + mListSections + ",mListOffset = " + mListOffset
                    + ",mListSectionLen = " + mListSectionLen+" mSectionIndexer="+mSectionIndexer);
        }
        
        setAbsentSections();
    }
    
    /**
     * Set absent section array, if the section in full sections is not
     * in the list sections, set absent flag to true, otherwise false.
     */
    private void setAbsentSections() {
        /*
         * If the section is not included in the list, set the corresponding
         * flag in mIsAbsent to true, else false.
         */
        for (int i = 0; i < mFullSectionLen; i++) {
            mIsAbsent[i] = true;
            for (int j = 0; j <= i && j < mListSectionLen; j++) {
                if (mListSections[j].equals(mFullSections[i])) {
                    mIsAbsent[i] = false;
                    break;
                }
            }
        }
    }
    
    /**
     * Set the rotate animation duration to the specified interval.
     * 
     * @param duration The animation duration want to be set.
     */
    public void setAnimationDuration(final int duration) {
        mAnimationDuration = duration;
    }
    
    /**
     * Get the rotate animation duration.
     * @return The duration of the rotate animation.
     */
    public int getAnimationDuration() {
        return mAnimationDuration;
    }
    
    /**
     * Set the paint color of present sections.
     */
    public void setEnableSectionColor(int color) {
    	mEnablePaintColor = color;
        mEnableSectionPaint.setColor(mEnablePaintColor);
    }
    
    /**
     * Set the paint color of absent sections.
     */
    public void setDisableSectionColor(int color) {
    	mDisablePaintColor = color;
        mDisableSectionPaint.setColor(mDisablePaintColor);
    }

    /**
     * Set the text color of present prompt window.
     */
    public void setEnablePromptColor(int color) {
    	mEnablePromptColor = color;
    }
    
    /**
     * Set the text color of absent prompt window.
     */
    public void setDisablePromptColor(int color) {
    	mDisablePromptColor = color;
    }

    /**
     * Start the window exit animation, and then dismiss the pop up window
     * after the animation has been played.
     */
    private void dismissPromptWindow() {
	/*begin add by zengxx 2012-02-14 */
        mPromptText.startAnimation(mExitAlphaAnim);
		 /*end add by zengxx 2012-02-14*/
		 
        mHandler.postDelayed(new Runnable() {
            public void run() {
                if (mPromptWindow != null && mPromptText != null) {
                    mPromptWindow.dismiss();
                } 
            }
        }, mAnimationDuration);
    }

    /**
     * Create the prompt window and then show it at appropriate position.
     */
    private void showPromptWindow() {
        /* Make sure we have a window before showing the pop up window. */
        if (getWindowVisibility() == View.VISIBLE) {
            createPromptWindow();
            positionPromptWindow();
        }
    }

    /**
     * Create the prompt window and set its background to be transparent.
     */
    private void createPromptWindow() {
        if (mPromptWindow == null) {
            Context c = getContext();
            PopupWindow p = new PopupWindow(c);

            p.setContentView(mPromptText);
            p.setWidth(LayoutParams.WRAP_CONTENT);
            p.setHeight(LayoutParams.WRAP_CONTENT);
            
            /* Set background of the prompt window to transparent. */
            ColorDrawable dw = new ColorDrawable(Color.TRANSPARENT);
            p.setBackgroundDrawable(dw);
          
            mPromptWindow = p;
        }
    }
    
    /**
     * Calculate the pop up window offset and position it in the right place.
     */
    private void positionPromptWindow() {
	/*begin add by zengxx 2012-02-14 */
	mPromptVertOffset = 0;
	/*end add by zengxx 2012-02-14*/
        if (DBG) {
            Log.i(TAG, "positionPopup: mPromptHorzOffset = " + mPromptHorzOffset + 
            		",mPromptVertOffset = " + mPromptVertOffset);
        }
        
        /* When touch event move out of the view, mHintText becomes null causes window leak. */
        if (mPromptText == null) {
            return;
        }
        
        /* Show the prompt window or update its position if it is already visible. */
        if (!mPromptWindow.isShowing()) {
            mPromptWindow.showAtLocation(this, 
                    Gravity.CENTER, mPromptHorzOffset, -mPromptVertOffset);
        } else {
            mPromptWindow.update(mPromptHorzOffset, -mPromptVertOffset, -1, -1);
        }
    }
}
