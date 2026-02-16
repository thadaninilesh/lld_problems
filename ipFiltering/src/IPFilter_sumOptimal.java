import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IPFilter_sumOptimal {

    List<String> patterns = new ArrayList<>();

    public void addPattern(String ipPattern) {
        patterns.addAll(Arrays.asList(ipPattern.split("\\.")));
    }

    public boolean matches(String ipAddress) {
        List<String> ipAddressParts = Arrays.asList(ipAddress.split("\\."));
        if (ipAddressParts.size() != patterns.size()) {
            return false;
        } else {
            for (int i = 0; i < patterns.size(); i++) {
                String patternPart = patterns.get(i);
                String ipPart = ipAddressParts.get(i);
                if (!patternPart.equals("*") && !patternPart.equals(ipPart)) {
                    return false;
                }
            }
            return true;
        }
    }

    public static void main(String[] args) {
        IPFilter_sumOptimal filter = new IPFilter_sumOptimal();
        filter.addPattern("192.168.*.*");

        if (filter.matches("192.168.10.43")) {
            System.out.println("Matched");
        }
        if (filter.matches("192.161.10.43")) {
            System.out.println("Matched");
        } else {
            System.out.println("Not Matched");
        }
        if (filter.matches("192.162.10.43")) {
            System.out.println("Matched");
        } else {
            System.out.println("Not Matched");
        }
        if (filter.matches("192.168.10")) {
            System.out.println("Matched");
        } else {
            System.out.println("Not Matched");
        }

    }

}
