package org.veil.gradle.plugins.jetty9

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.WarPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import static org.hamcrest.Matchers.*
import static org.hamcrest.CoreMatchers.is
import static org.junit.Assert.*

class JettyPluginTest {
    private final Project project = ProjectBuilder.builder().withProjectDir(new File("build/tmp/tests")).build()

    @Test
    public void appliesWarPluginAndAddsConventionToProject() {
        new Jetty9Plugin().apply(project)

        assertTrue(project.getPlugins().hasPlugin(WarPlugin))

        assertThat(project.convention.plugins.jetty9, instanceOf(JettyPluginConvention))
    }
    
    @Test
    public void addsTasksToProject() {
        new Jetty9Plugin().apply(project)

        def task = project.tasks[Jetty9Plugin.JETTY_RUN]
        assertThat(task, instanceOf(JettyRun))
        assertTrue(isTaskDependsOnOtherTask(task, JavaPlugin.CLASSES_TASK_NAME))
        assertThat(task.httpPort, equalTo(project.httpPort))

        task = project.tasks[Jetty9Plugin.JETTY_RUN_WAR]
        assertThat(task, instanceOf(JettyRunWar))
        assertTrue(isTaskDependsOnOtherTask(task, WarPlugin.WAR_TASK_NAME))
        assertThat(task.httpPort, equalTo(project.httpPort))

        task = project.tasks[Jetty9Plugin.JETTY_STOP]
        assertThat(task, instanceOf(JettyStop))
        assertThat(task.stopPort, equalTo(project.stopPort))
    }

    @Test
    public void addsMappingToNewJettyTasks() {
        new Jetty9Plugin().apply(project)

        def task = project.tasks.create('customRun', JettyRun)
        assertTrue(isTaskDependsOnOtherTask(task, JavaPlugin.CLASSES_TASK_NAME))
        assertThat(task.httpPort, equalTo(project.httpPort))

        task = project.tasks.create('customWar', JettyRunWar)
        assertTrue(isTaskDependsOnOtherTask(task, WarPlugin.WAR_TASK_NAME))
        assertThat(task.httpPort, equalTo(project.httpPort))
    }

    private boolean isTaskDependsOnOtherTask(Task task, String otherTaskName) {
        boolean result = false;
        task.getTaskDependencies().getDependencies(task).each {
            dep -> if (dep.name.equals(otherTaskName)) {result = true; return;}
        }
        return result;
    }

}
