package org.jbei.ice.lib.executor;

import org.jbei.ice.lib.dao.hibernate.HibernateHelper;
import org.jbei.ice.lib.logging.Logger;

/**
 * Runnable for running tasks
 *
 * @author Hector Plahar
 */
class TaskHandler implements Runnable {
    private final Task task;

    public TaskHandler(Task task) {
        this.task = task;
    }

    @Override
    public void run() {
        try {
            HibernateHelper.beginTransaction();
            task.execute();
            HibernateHelper.commitTransaction();
        } catch (Throwable caught) {
            Logger.error(caught);
            HibernateHelper.rollbackTransaction();
        }
    }
}