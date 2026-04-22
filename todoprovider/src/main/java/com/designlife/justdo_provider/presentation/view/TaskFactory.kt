package com.designlife.justdo_provider.presentation.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.toArgb
import com.designlife.justdo_provider.R
import com.designlife.justdo_provider.SetworkProvider
import com.designlife.justdo_provider.data.ProviderTask
import com.designlife.justdo_provider.common.ProviderUtils

class TaskFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private val tasks = mutableListOf<ProviderTask>()

    override fun onCreate() {
    }

    override fun onDataSetChanged() {
        tasks.clear()
        tasks.addAll(SetworkProvider.tasks)
    }

    override fun getCount(): Int = tasks.size

    @SuppressLint("IntentReset")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun getViewAt(position: Int): RemoteViews {
        val view = RemoteViews(
            context.packageName,
            R.layout.item_task
        )
        view.setTextViewText(R.id.taskTitle, tasks[position].title)
        view.setTextViewText(R.id.taskDescription, tasks[position].description)
        view.setTextViewText(R.id.taskTime, ProviderUtils.getTimeGracefully(tasks[position].time))
        view.setInt(R.id.taskStatusDot, "setColorFilter", tasks[position].color.toArgb())

        val task = tasks[position]

        val fillInIntent = Intent().apply {
            putExtra(ProviderUtils.TASK_ID, task.id.toInt())
        }
        view.setOnClickFillInIntent(R.id.taskItem, fillInIntent)

        return view
    }

    override fun onDestroy() {
        tasks.clear()
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true


}