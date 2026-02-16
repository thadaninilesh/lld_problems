/*
* Given an integer array nums, return all the different possible non-decreasing subsequences of the given array with at least two elements. You may return the answer in any order.
Example 1:

Input: nums = [4,6,7,7]
Output: [[4,6],[4,6,7],[4,6,7,7],[4,7],[4,7,7],[6,7],[6,7,7],[7,7]]
Example 2:

Input: nums = [4,4,3,2,1]
Output: [[4,4]]
* */

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class NonDecreasingSubsequences {

    public static void main(String[] args) {
        List<List<Integer>> subsequences = new NonDecreasingSubsequences().findSubsequences(new int[]{4, 6, 7, 7});
        System.out.println(subsequences);
    }

    public List<List<Integer>> findSubsequences(int[] nums) {
        List<List<Integer>> result = new LinkedList<>();
        helper(new LinkedList<>(), 0, nums, result);
        return result;
    }

    private void helper(LinkedList<Integer> list, int index, int [] nums, List<List<Integer>> result) {

        if (list.size() > 1)
            result.add(new LinkedList<>(list));

        Set<Integer> used = new HashSet<>();
        for (int i = index; i < nums.length; i++){
            if (used.contains(nums[i]))
                continue;

            if(list.isEmpty() || nums[i] >= list.peekLast()) {
                used.add(nums[i]);
                list.add(nums[i]);
                helper(list, i+1, nums, result);
                list.remove(list.size() - 1);
            }
        }
    }

}
