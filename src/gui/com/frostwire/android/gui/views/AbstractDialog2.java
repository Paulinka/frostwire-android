/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.android.gui.views;

import java.lang.ref.WeakReference;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager.LayoutParams;

import com.frostwire.util.Ref;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public abstract class AbstractDialog2 extends DialogFragment {

    /**
     * The identifier for the positive button.
     */
    public static final int BUTTON_POSITIVE = -1;

    /**
     * The identifier for the negative button. 
     */
    public static final int BUTTON_NEGATIVE = -2;

    private final String tag;
    private final int layoutResId;

    private WeakReference<Activity> activityRef;

    public AbstractDialog2(String tag, int layoutResId) {
        if (layoutResId == 0) {
            throw new RuntimeException("Resource id can't be 0");
        }

        this.tag = tag;
        this.layoutResId = layoutResId;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        activityRef = Ref.weak(activity);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dlg = super.onCreateDialog(savedInstanceState);

        setContentView(dlg, layoutResId);
        initComponents(dlg, savedInstanceState);

        return dlg;
    }

    public void show(FragmentManager manager) {
        super.show(manager, tag);
    }

    public void performDialogClick(int which) {
        if (Ref.alive(activityRef)) {
            dispatchDialogClick(activityRef.get(), tag, which);
        }
    }

    public void performPositiveClick() {
        performDialogClick(BUTTON_POSITIVE);
    }

    public void performNegativeClick() {
        performDialogClick(BUTTON_NEGATIVE);
    }

    protected void setContentView(Dialog dlg, int layoutResId) {
        dlg.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        dlg.setContentView(layoutResId);
    }

    protected abstract void initComponents(Dialog dlg, Bundle savedInstanceState);

    @SuppressWarnings("unchecked")
    protected final <T extends View> T findView(Dialog dlg, int id) {
        return (T) dlg.findViewById(id);
    }

    private void dispatchDialogClick(Activity activity, String tag, int which) {
        dispatchDialogClickSafe(activity, tag, which);

        if (activity instanceof AbstractActivity) {
            List<Fragment> fragments = ((AbstractActivity) activity).getVisibleFragments();

            for (Fragment f : fragments) {
                dispatchDialogClickSafe(f, tag, which);
            }
        }
    }

    private void dispatchDialogClickSafe(Object obj, String tag, int which) {
        if (obj instanceof OnDialogClickListener) {
            ((OnDialogClickListener) obj).onDialogClick(tag, which);
        }
    }

    public interface OnDialogClickListener {

        public void onDialogClick(String tag, int which);
    }
}
