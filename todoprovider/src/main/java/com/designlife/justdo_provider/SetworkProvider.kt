package com.designlife.justdo_provider

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.designlife.justdo_provider.data.ProviderTask
import com.designlife.justdo_provider.presentation.AppWidget

class SetworkProvider(
    private val context: Context
) {
    fun addTask(task: ProviderTask){
        SetworkProvider.setTask(task)
        updateRemoteView()
    }
    
    fun removeTask(task: ProviderTask){
        SetworkProvider.tasks.removeIf { stask -> stask.id == task.id }
        updateRemoteView()
    }

    fun addTaskList(tasks: List<ProviderTask>){
        SetworkProvider.setTasks(tasks)
        updateRemoteView()
    }

    private fun updateRemoteView(){
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, AppWidget::class.java)

        manager.notifyAppWidgetViewDataChanged(
            manager.getAppWidgetIds(component),
            R.id.listView
        )
    }


    companion object{
        internal val tasks = mutableListOf<ProviderTask>()

        private fun setTasks(newTasks: List<ProviderTask>) {
            tasks.clear()
            tasks.addAll(newTasks)
        }

        private fun setTask(newTasks: ProviderTask) {
            tasks.add(newTasks)
        }
    }
}

