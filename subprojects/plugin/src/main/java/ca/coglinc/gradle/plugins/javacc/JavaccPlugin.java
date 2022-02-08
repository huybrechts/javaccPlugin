package ca.coglinc.gradle.plugins.javacc;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import org.gradle.api.Action;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.file.FileTree;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.compile.JavaCompile;

public class JavaccPlugin implements Plugin<Project> {
    public static final String GROUP = "JavaCC";

    @Override
    public void apply(Project project) {
        Configuration configuration = createJavaccConfiguration(project);
        configureDefaultJavaccDependency(project, configuration);

        addCompileJavaccTaskToProject(project, configuration);
        addCompileJJTreeTaskToProject(project, configuration);
        addCompileJjdocTaskToProject(project, configuration);

        JavaToJavaccDependencyAction compileJavaDependsOnCompileJavacc = new JavaToJavaccDependencyAction();
        project.afterEvaluate(compileJavaDependsOnCompileJavacc);
    }

    private Configuration createJavaccConfiguration(Project project) {
        Configuration configuration = project.getConfigurations().create("javacc");
        configuration.setVisible(false);
        configuration.setTransitive(true);
        configuration.setDescription("The javacc dependencies to be used.");
        return configuration;
    }

    private void configureDefaultJavaccDependency(final Project project, Configuration configuration) {
        configuration.defaultDependencies(new Action<DependencySet>() {
            @Override
            public void execute(DependencySet dependencies) {
                dependencies.add(project.getDependencies().create("net.java.dev.javacc:javacc:6.1.2"));
            }
        });
    }

    private void addCompileJavaccTaskToProject(Project project, Configuration configuration) {
        addTaskToProject(project, CompileJavaccTask.class, CompileJavaccTask.TASK_NAME_VALUE, CompileJavaccTask.TASK_DESCRIPTION_VALUE,
            JavaccPlugin.GROUP, configuration);
    }

    private void addCompileJJTreeTaskToProject(Project project, Configuration configuration) {
        addTaskToProject(project, CompileJjTreeTask.class, CompileJjTreeTask.TASK_NAME_VALUE, CompileJjTreeTask.TASK_DESCRIPTION_VALUE,
            JavaccPlugin.GROUP, configuration);
    }

    private void addCompileJjdocTaskToProject(Project project, Configuration configuration) {
        addTaskToProject(project, CompileJjdocTask.class, CompileJjdocTask.TASK_NAME_VALUE, CompileJjdocTask.TASK_DESCRIPTION_VALUE,
            JavaccPlugin.GROUP, configuration);
    }

    private void addTaskToProject(
        final Project project,
        Class<? extends AbstractJavaccTask> type,
        String name,
        final String description,
        final String group,
        final Configuration configuration) {


        project.getTasks().register(name, type,
            new Action<AbstractJavaccTask>() {
                @Override public void execute(@Nonnull AbstractJavaccTask task) {
                    task.setDescription(description);
                    task.setGroup(group);
                    task.getClasspath().setFrom(configuration);
                    FileTree sourceTree = getJavaSourceTree(project, task.getOutputDirectory());
                    task.setJavaSourceTree(sourceTree);
                }
            });
    }

    private FileTree getJavaSourceTree(Project project, File outputDirectory) {
        FileTree javaSourceTree = null;
        TaskCollection<JavaCompile> javaCompileTasks = project.getTasks().withType(JavaCompile.class);

        for (JavaCompile task : javaCompileTasks) {
            if (javaSourceTree == null) {
                javaSourceTree = task.getSource();
            } else {
                javaSourceTree = javaSourceTree.plus(task.getSource());
            }
        }

        return excludeOutputDirectory(javaSourceTree, outputDirectory);
    }

    private FileTree excludeOutputDirectory(FileTree sourceTree, final File outputDirectory) {
        if (sourceTree == null) {
            return null;
        }

        Spec<File> outputDirectoryFilter = new Spec<File>() {

            @Override
            public boolean isSatisfiedBy(File file) {
                return file.getAbsolutePath().contains(outputDirectory.getAbsolutePath());
            }
        };

        sourceTree = sourceTree.minus(sourceTree.filter(outputDirectoryFilter)).getAsFileTree();
        return sourceTree;
    }


}
