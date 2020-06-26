import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Description: simple shell implemented in Java
 *
 * Details
 * --------
 * Built-in commands:
 *      - cd <dir>: change directory
 *      - list: list detailed file info for current directory
 *      - ptime: print total time spent executing child processes in session
 *      - history: prints session command history
 *      - ^ <int>: executes command <int> from history
 *      - exit: exits shell
 *
 * Support for external commands:
 *      - one-to-one piping by placing "|" between external commands
 *      - thread unblocking by adding "&" at end of command
 *
 * @author ZionSteiner
 */
public class Shell {
    public static void main(String args[]) {
        ArrayList<String> cmdHistory = new ArrayList<>();   // Tracks entered commands
        long exeTime = 0;                                   // Tracks time spent executing external commands

        Scanner input = new Scanner(System.in);

        while (true) {
            String currentDir = System.getProperty("user.dir");
            String homeDir = System.getProperty("user.home");

            // Print prompt [<currentDir>]
            System.out.printf("[%s]: ", currentDir);

            // Get cmd with args
            String cmd = input.nextLine();

            // Re-loop if no cmd entered
            if (cmd.equals("")) {
                continue;
            }

            // Send args to directory
            exeTime += directory(cmd, cmdHistory, exeTime);
        }
    }

    /**
     * Type: directory
     * Directs arguments to the correct function
     *
     * @param cmdArgs array of parsed command and arguments
     * @param cmdHistory arrayList of commands used in session
     * @param exeTime records time spent executing external commands
     * @author ZionSteiner
     */
    private static long directory(String cmd, ArrayList<String> cmdHistory, long exeTime) {

        // Add cmd to history
        cmdHistory.add(cmd);

        // Parse command and args
        String[] cmdArgs = splitCommand(cmd);


        // Direct command
        switch (cmdArgs[0]) {
            case "list":
                listFiles(cmdArgs);
                break;
            case "ptime":
                ptime(exeTime);
                break;
            case "history":
                history(cmdHistory);
                break;
            case "^":
                executeCmdFrmHistory(cmdArgs, cmdHistory, exeTime);
                break;
            case "cd":
                cd(cmdArgs);
                break;
            case "exit":
                System.exit(0);
                break;
            default:
                exeTime = handleExtCmd(cmdArgs);
                return exeTime;
        }
        return 0;
    }

    /**
     * Type: built-in command
     * cd: change directory
     * options:
     *          cd      - return to user.home
     *          cd ..   - return to parent directory
     *          cd <dir>  - go to currentDir/dir
                                                                                         *
     * @param cmdArgs array of parsed command and args
     * @author ZionSteiner
     */
    private static void cd(String[] cmdArgs) {
        String homeDir = System.getProperty("user.home");
        String currentDir = System.getProperty("user.dir");

        // cd
        if ( (cmdArgs.length == 1) || (cmdArgs.length == 2 && cmdArgs[1].equals("&")) ) {
            System.setProperty("user.dir", homeDir);
        // Too many args
        } else if ( (cmdArgs.length > 2 && !cmdArgs[2].equals("&")) || (cmdArgs.length > 3 && cmdArgs[2].equals("&")) ) {
            System.out.println("cd: too many arguments");
        // cd ..
        } else if (cmdArgs[1].equals("..")) {
            File fileDir = new File(currentDir);
            try {
                System.setProperty("user.dir", fileDir.getParent());
            } catch (NullPointerException ex) {
                System.out.println("cd: already at rock bottom");
            }
        // cd <dir>
        } else {
            java.nio.file.Path updtdPath = java.nio.file.Paths.get(currentDir, cmdArgs[1]);
            File pathFile = updtdPath.toFile();

            if (pathFile.exists()) {
                System.setProperty("user.dir", updtdPath.toString());
            } else {
                System.out.println("cd: directory \"" + cmdArgs[1] + "\" doesn't exist");
            }
        }
    }

    /**
     * Type: built-in command
     * list: list file info from current directory
     *
     * @param cmdArgs array of parsed command and args
     * @author ZionSteiner
     */
    private static void listFiles(String[] cmdArgs) {
        String currentDir = System.getProperty("user.dir");
        File fileDir = new File(currentDir);

        File[] files = fileDir.listFiles();

        for (File file : files) {
            // Permissions
            boolean isDirectory = file.isDirectory();
            boolean canRead = file.canRead();
            boolean canWrite = file.canWrite();
            boolean canExecute = file.canExecute();

            String permissions = buildPermissionsStr(isDirectory, canRead, canWrite, canExecute);

            // Size in bytes
            long bytes = file.length();

            // Last modified
            long lastModified = file.lastModified();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm");
            String date = sdf.format(lastModified);

            // Name
            String name = file.getName();

            // Ex. drwx       4080 Feb 4, 2019 21:44 doc.txt
            System.out.printf("%s %10d %s %s \n", permissions, bytes, date, name);
        }
    }

    /**
     * Type: built-in command
     * ptime: lists total time child processes have spent running in this session
     *
     * @param exeTime
     */
    private static void ptime(long exeTime) {
        System.out.printf("Total time spent in child processes: %.4f seconds \n", ((float)(exeTime) / 1000));
    }

    /**
     * Type: built-in command
     * history: list all commands used in current session
     * @param cmdHistory arrayList of commands used in session
     * @author ZionSteiner
     */
    private static void history(ArrayList<String> cmdHistory) {
        System.out.println("-- Command History --");

        int i = 1;
        for (String cmd : cmdHistory) {
            System.out.printf("%d : %s \n", i++, cmd);
        }
    }

    /**
     * Type: built-in command
     * ^ <int>: executes command <int> from session history
     *
     * @param cmdArgs array of parsed command and arguments
     * @param cmdHistory arrayList of commands used in session
     * @author ZionSteiner
     */
    private static void executeCmdFrmHistory(String[] cmdArgs, ArrayList<String> cmdHistory, long exeTime) {
        // No arguments entered
        if (cmdArgs.length == 1) {
            System.out.println("^: no arguments found");
        // ^ <int>
        } else if (cmdArgs.length == 2 || cmdArgs[2].equals("&")) {
            int cmdNum = -1;

            try {
                cmdNum = Integer.parseInt(cmdArgs[1]);
            } catch (NumberFormatException ex) {
                System.out.println("^: illegal argument");
            }

            if (cmdNum < cmdHistory.size() && cmdNum > 0) {
                String cmd = cmdHistory.get(cmdNum - 1);
                directory(cmd, cmdHistory, exeTime);
            } else {
                if (cmdNum != -1) {
                    System.out.printf("^:  index \"%d\" not found in history \n", cmdNum);
                }
            }
         // Too many arguments
        } else {
            System.out.println("^: too many arguments");
        }
    }

    /**
     * Type: external command handler
     * Redirects external command to OS
     *
     * @param cmdArgs array of parsed command and args
     * @return execution time (ms) of external commands
     * @author ZionSteiner
     */
    private static long handleExtCmd(String[] cmdArgs) {
        long exeTime = 0;

        // Check for pipe
        int pipeIndex = -1;
        for (int i = 0; i < cmdArgs.length; i++) {
            if (cmdArgs[i].equals("|")) {
                pipeIndex = i;
                break;
            }
        }

        if (pipeIndex != -1) {
            exeTime += pipedExtCmd(cmdArgs, pipeIndex);
        } else {
            exeTime += singleExtCmd(cmdArgs);
        }

        return exeTime;
    }

    /**
     * Type: helper for handleExtCmd()
     * Handles single external commands
     *
     * @param cmdArgs array of parsed command and args
     * @return execution time (ms) of external commands
     * @author ZionSteiner
     */
    private static long singleExtCmd(String[] cmdArgs) {
        boolean wait = true;    // Wait to finish command before continuing thread
        long startTime = 0;     // Start time of external execution
        long endTime = 0;       // End time of external execution
        long time = 0;

        if (cmdArgs[cmdArgs.length - 1].equals("&")) {
            cmdArgs = Arrays.copyOfRange(cmdArgs, 0, cmdArgs.length - 1);
            wait = false;
        }

        ProcessBuilder pb = new ProcessBuilder(cmdArgs);
        pb.inheritIO();

        File cwd = new File(System.getProperty("user.dir"));
        pb.directory(cwd);

        try {
            startTime = System.currentTimeMillis();
            Process p = pb.start();
            if (wait) {
                p.waitFor();
            }
            endTime = System.currentTimeMillis();
            time = endTime - startTime;
        } catch (IOException ex) {
            System.out.println("Illegal command");
        } catch (Exception ex) {
            System.out.println("Error: something happened");
        }

        return time;
    }

    /**
     * Type: helper for handleExtCmd()
     * Handles piped external commands
     *
     * @param cmdArgs array containing parsed command and args
     * @param pipeIndex
     * @return execution time of child processes
     * @author ZionSteiner
     */
    private static long pipedExtCmd(String[] cmdArgs, int pipeIndex) {
        boolean wait = true;
        long startTime = 0;
        long endTime = 0;
        long time = 0;

        String[] cmd1 = Arrays.copyOfRange(cmdArgs, 0, pipeIndex);
        String[] cmd2 = Arrays.copyOfRange(cmdArgs,pipeIndex + 1, cmdArgs.length);

        if (cmd2[cmd2.length - 1].equals("&")) {
            cmd2 = Arrays.copyOfRange(cmd2, 0, cmd2.length - 1);
            wait = false;
        }


        ProcessBuilder pb1 = new ProcessBuilder(cmd1);
        pb1.redirectInput(ProcessBuilder.Redirect.INHERIT);

        ProcessBuilder pb2 = new ProcessBuilder(cmd2);
        pb2.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        // Set process cwd
        File cwd = new File(System.getProperty("user.dir"));
        pb1.directory(cwd);
        pb2.directory(cwd);

        try {
            startTime = System.currentTimeMillis();
            Process p1 = pb1.start();
            Process p2 = pb2.start();

            java.io.InputStream in = p1.getInputStream();
            java.io.OutputStream out = p2.getOutputStream();

            int c;
            while ((c = in.read()) != -1) {
                out.write(c);
            }

            out.flush();
            out.close();

            if (wait) {
                p1.waitFor();
                p2.waitFor();
            }

            endTime = System.currentTimeMillis();
            time = endTime - startTime;
        } catch (IOException ex) {
            System.out.println("Illegal command");
        } catch (Exception ex) {
            System.out.println("Error: something happened");
        }

        return time;
    }

    /**
     * Type: helper for listFiles()
     * Builds permissions string given boolean permissions
     * @param isDirectory
     * @param canRead
     * @param canWrite
     * @param canExecute
     * @return permissions string
     */
    private static String buildPermissionsStr(boolean isDirectory, boolean canRead, boolean canWrite, boolean canExecute) {
        String d;
        String r;
        String w;
        String x;

        if (isDirectory) {
            d = "d";
        } else {
            d = "-";
        }

        if (canRead) {
            r = "r";
        } else {
            r = "-";
        }

        if (canWrite) {
            w = "w";
        } else {
            w = "-";
        }

        if (canExecute) {
            x = "x";
        } else {
            x = "-";
        }

        String permissions = d + r + w + x;

        return permissions;

    }

    /**
     * Split the user cmd by spaces, but preserving them when inside double-quotes.
     * Adapted code borrowed from: https://usu.instructure.com/courses/529303/assignments/2574806
     * Code Adapted from: https://stackoverflow.com/questions/366202/regex-for-splitting-a-string-using-space-when-not-surrounded-by-single-or-double
     */
    public static String[] splitCommand(String cmd) {
        java.util.List<String> matchList = new java.util.ArrayList<>();

        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(cmd);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Add double-quoted string without the quotes
                matchList.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                // Add single-quoted string without the quotes
                matchList.add(regexMatcher.group(2));
            } else {
                // Add unquoted word
                matchList.add(regexMatcher.group());
            }
        }

        return matchList.toArray(new String[matchList.size()]);

    }
}
