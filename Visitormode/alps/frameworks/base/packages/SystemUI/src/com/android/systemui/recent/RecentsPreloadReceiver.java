/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.recent;

import com.android.systemui.recents.model.RecentsTaskLoader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RecentsPreloadReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (RecentsActivity.PRELOAD_INTENT.equals(intent.getAction())) {
            RecentTasksLoader.getInstance(context).preloadRecentTasksList();
        } else if (RecentsActivity.CANCEL_PRELOAD_INTENT.equals(intent.getAction())){
            RecentTasksLoader.getInstance(context).cancelPreloadingRecentTasksList();
        }
        /*liuhao added for visitor mode 20150403 begin*/
        else if ("malata_visitor_mode_change".equals(intent.getAction())) {
            RecentsTaskLoader.getInstance().setVisitorModeIsChange(true);
        }
        /*liuhao added for visitor mode 20150403 end*/
    }
}
