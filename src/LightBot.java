import java.util.*;

public class LightBot {
    private final char[][] initialMap;
    private final int rows;
    private final int cols;
    private final int startRow;
    private final int startCol;
    private final char startDir;

    private char[][] map;
    private int row;
    private int col;
    private char dir;

    private Map<String, Function> functions;

    private static class Function {
        final List<String> params;
        final List<String> body;
        Function(List<String> params, List<String> body) {
            this.params = params;
            this.body = body;
        }
    }

    public LightBot(String[] grid) {
        this.rows = grid.length;
        this.cols = grid[0].length();
        this.initialMap = new char[rows][cols];
        int rPos = 0, cPos = 0;
        char d = 'R';
        // parse initial grid
        for (int r = 0; r < rows; r++) {
            String line = grid[r];
            for (int c = 0; c < cols; c++) {
                char ch = line.charAt(c);
                if (ch == 'R' || ch == 'L' || ch == 'U' || ch == 'D') {
                    rPos = r;
                    cPos = c;
                    d = ch;
                    initialMap[r][c] = '.';
                } else {
                    initialMap[r][c] = ch;
                }
            }
        }
        this.startRow = rPos;
        this.startCol = cPos;
        this.startDir = d;
        reset();
    }

    public void reset() {
        // reset map and robot state
        this.map = new char[rows][cols];
        for (int r = 0; r < rows; r++) {
            System.arraycopy(initialMap[r], 0, map[r], 0, cols);
        }
        this.row = startRow;
        this.col = startCol;
        this.dir = startDir;
    }

    public void runProgram(String[] instructions) {
        // parse definitions and top-level commands
        functions = new HashMap<>();
        List<String> commands = new ArrayList<>();
        for (int i = 0; i < instructions.length; i++) {
            String instr = instructions[i].trim();
            if (instr.startsWith("FUNCTION")) {
                String header = instr.substring(8).trim();
                String name;
                List<String> params = new ArrayList<>();
                int p = header.indexOf('(');
                if (p >= 0) {
                    name = header.substring(0, p).trim();
                    String inside = header.substring(p + 1, header.indexOf(')', p)).trim();
                    if (!inside.isEmpty()) {
                        for (String param : inside.split(",")) {
                            params.add(param.trim());
                        }
                    }
                } else {
                    name = header;
                }
                List<String> body = new ArrayList<>();
                // collect until ENDFUNCTION
                i++;
                while (i < instructions.length && !instructions[i].trim().equals("ENDFUNCTION")) {
                    body.add(instructions[i].trim());
                    i++;
                }
                functions.put(name, new Function(params, body));
            } else {
                commands.add(instr);
            }
        }
        // execute
        executeCommands(commands, new HashMap<>());
    }

    private void executeCommands(List<String> cmds, Map<String, Integer> vars) {
        for (int i = 0; i < cmds.size(); i++) {
            String instr = cmds.get(i);
            if (instr.equals("LEFT")) {
                turnLeft();
            } else if (instr.equals("RIGHT")) {
                turnRight();
            } else if (instr.equals("FORWARD")) {
                moveForward();
            } else if (instr.equals("LIGHT")) {
                lightCell();
            } else if (instr.startsWith("REPEAT")) {
                String[] parts = instr.split(" ", 2);
                int count = eval(parts[1], vars);
                // find matching ENDREPEAT
                List<String> block = new ArrayList<>();
                int depth = 1;
                i++;
                while (i < cmds.size() && depth > 0) {
                    String line = cmds.get(i);
                    if (line.startsWith("REPEAT")) {
                        depth++;
                        block.add(line);
                    } else if (line.equals("ENDREPEAT")) {
                        depth--;
                        if (depth > 0) block.add(line);
                    } else {
                        block.add(line);
                    }
                    i++;
                }
                // execute block count times
                for (int k = 0; k < count; k++) {
                    executeCommands(block, vars);
                }
                i--; // adjust to last processed
            } else if (instr.startsWith("CALL")) {
                String rest = instr.substring(4).trim();
                String fname;
                List<Integer> args = new ArrayList<>();
                int p = rest.indexOf('(');
                if (p >= 0) {
                    fname = rest.substring(0, p).trim();
                    String inside = rest.substring(p + 1, rest.indexOf(')', p)).trim();
                    if (!inside.isEmpty()) {
                        for (String token : inside.split(",")) {
                            args.add(eval(token.trim(), vars));
                        }
                    }
                } else {
                    fname = rest;
                }
                Function func = functions.get(fname);
                Map<String, Integer> newVars = new HashMap<>();
                for (int idx = 0; idx < func.params.size(); idx++) {
                    newVars.put(func.params.get(idx), args.get(idx));
                }
                executeCommands(func.body, newVars);
            }
        }
    }

    private int eval(String token, Map<String, Integer> vars) {
        if (vars.containsKey(token)) return vars.get(token);
        return Integer.parseInt(token);
    }

    private void turnLeft() {
        switch (dir) {
            case 'R': dir = 'U'; break;
            case 'U': dir = 'L'; break;
            case 'L': dir = 'D'; break;
            case 'D': dir = 'R'; break;
        }
    }

    private void turnRight() {
        switch (dir) {
            case 'R': dir = 'D'; break;
            case 'D': dir = 'L'; break;
            case 'L': dir = 'U'; break;
            case 'U': dir = 'R'; break;
        }
    }

    private void moveForward() {
        int dr = 0, dc = 0;
        switch (dir) {
            case 'R': dc = 1; break;
            case 'L': dc = -1; break;
            case 'U': dr = -1; break;
            case 'D': dr = 1; break;
        }
        row = (row + dr + rows) % rows;
        col = (col + dc + cols) % cols;
    }

    private void lightCell() {
        char c = map[row][col];
        if (c == 'O') {
            map[row][col] = 'X';
        } else if (c == '.') {
            map[row][col] = 'x';
        }
    }

    public int[] getRobotPosition() {
        return new int[]{col, row};
    }

    public String[] getMap() {
        String[] out = new String[rows];
        for (int r = 0; r < rows; r++) {
            out[r] = new String(map[r]);
        }
        return out;
    }
}
