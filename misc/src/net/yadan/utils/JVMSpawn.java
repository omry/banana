/*
 * Copyright (C) ${year} Omry Yadan <${email}>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Commandline.Argument;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.StringUtils;

public class JVMSpawn {

  public static int spawn(String jvmName, String className, String vmArgs, String[] args) {
    int ret = -1;
    // global ant project settings
    Project project = new Project();
    project.setBaseDir(new File(System.getProperty("user.dir")));
    project.init();
    BuildLogger logger = new MyLogger();
    project.addBuildListener(logger);
    logger.setOutputPrintStream(System.out);
    logger.setErrorPrintStream(System.err);
    logger.setMessageOutputLevel(Project.MSG_INFO);
    project.fireBuildStarted();

    Throwable caught = null;
    try {
      Java javaTask = new Java();
      javaTask.setNewenvironment(true);
      javaTask.setTaskName(jvmName);
      javaTask.setProject(project);
      javaTask.setFork(true);
      javaTask.setFailonerror(true);
      javaTask.setClassname(className);

      Argument jvmArgs = javaTask.createJvmarg();
      jvmArgs.setLine(vmArgs);

      Argument taskArgs = javaTask.createArg();
      taskArgs.setLine(Util.implode(args, " "));

      // use same classpath as current jvm
      Path classPath = new Path(project, System.getProperty("java.class.path"));
      javaTask.setClasspath(classPath);

      javaTask.init();
      ret = javaTask.executeJava();
    } catch (BuildException e) {
      caught = e;
    }
    project.fireBuildFinished(caught);
    return ret;
  }

  static class MyLogger implements BuildLogger {

    protected int msgOutputLevel = Project.MSG_INFO;

    public static final int LEFT_COLUMN_SIZE = 12;

    /** PrintStream to write non-error messages to */
    protected PrintStream out;

    /** PrintStream to write error messages to */
    protected PrintStream err;

    @Override
    public void buildStarted(BuildEvent event) {
    }

    @Override
    public void buildFinished(BuildEvent event) {
    }

    @Override
    public void targetStarted(BuildEvent event) {
    }

    @Override
    public void targetFinished(BuildEvent event) {
    }

    @Override
    public void taskStarted(BuildEvent event) {
    }

    @Override
    public void taskFinished(BuildEvent event) {
    }

    @Override
    public void messageLogged(BuildEvent event) {
      int priority = event.getPriority();
      // Filter out messages based on priority
      if (priority <= msgOutputLevel) {

        StringBuffer message = new StringBuffer();
        if (event.getTask() != null) {
          // Print out the name of the task if we're in one
          String name = event.getTask().getTaskName();
          String label = "[" + name + "] ";
          int size = LEFT_COLUMN_SIZE - label.length();
          StringBuffer tmp = new StringBuffer();
          for (int i = 0; i < size; i++) {
            tmp.append(" ");
          }
          tmp.append(label);
          label = tmp.toString();

          try {
            BufferedReader r = new BufferedReader(new StringReader(event.getMessage()));
            String line = r.readLine();
            boolean first = true;
            do {
              if (first) {
                if (line == null) {
                  message.append(label);
                  break;
                }
              } else {
                message.append(StringUtils.LINE_SEP);
              }
              first = false;
              message.append(label).append(line);
              line = r.readLine();
            } while (line != null);
          } catch (IOException e) {
            // shouldn't be possible
            message.append(label).append(event.getMessage());
          }
        } else {
          message.append(event.getMessage());
        }
        Throwable ex = event.getException();
        if (ex != null) {
          message.append(StringUtils.getStackTrace(ex));
        }

        String msg = message.toString();
        if (priority != Project.MSG_ERR) {
          out.println(msg);
        } else {
          out.println(msg);
        }
      }
    }

    @Override
    public void setMessageOutputLevel(int level) {
    }

    @Override
    public void setEmacsMode(boolean emacsMode) {
    }

    @Override
    public void setOutputPrintStream(PrintStream output) {
      this.out = new PrintStream(output, true);
    }

    @Override
    public void setErrorPrintStream(PrintStream err) {
      this.err = new PrintStream(err, true);
    }
  }
}