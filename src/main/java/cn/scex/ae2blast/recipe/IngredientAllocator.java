package cn.scex.ae2blast.recipe;
import java.util.*;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/** Small integral max-flow: handles overlapping tags and repeated/count ingredients without greedy failures. */
public final class IngredientAllocator {
    public static int[] allocate(IItemHandler inventory, List<SizedIngredient> ingredients) {
        int slots = 9, n = slots + ingredients.size() + 2, sink = n - 1;
        if (ingredients.isEmpty() || ingredients.size() > 9) return null;
        int[][] capacity = new int[n][n]; int required = 0;
        for (int s = 0; s < slots; s++) {
            var stack = inventory.getStackInSlot(s);
            capacity[0][1+s] = stack.getCount();
            for (int i = 0; i < ingredients.size(); i++) if (ingredients.get(i).ingredient().test(stack)) capacity[1+s][10+i] = stack.getCount();
        }
        for (int i = 0; i < ingredients.size(); i++) { int c = ingredients.get(i).count(); if (c < 1 || c > 64) return null; capacity[10+i][sink] = c; required += c; }
        int flow = 0;
        while (flow < required) {
            int[] parent = new int[n]; Arrays.fill(parent, -1); parent[0] = 0;
            int[] queue = new int[n]; int head = 0, tail = 1;
            while (head < tail && parent[sink] < 0) {
                int u = queue[head++];
                for (int v = 1; v < n; v++) if (parent[v] < 0 && capacity[u][v] > 0) { parent[v] = u; queue[tail++] = v; }
            }
            if (parent[sink] < 0) return null;
            int amount = required - flow;
            for (int v = sink; v != 0; v = parent[v]) amount = Math.min(amount, capacity[parent[v]][v]);
            for (int v = sink; v != 0; v = parent[v]) { capacity[parent[v]][v] -= amount; capacity[v][parent[v]] += amount; }
            flow += amount;
        }
        int[] consumed = new int[9];
        for (int s = 0; s < 9; s++) consumed[s] = inventory.getStackInSlot(s).getCount() - capacity[0][1+s];
        return consumed;
    }
    private IngredientAllocator() {}
}
