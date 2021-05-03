import com.mergebase.util.Java2Json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class Example {

    /*
    Prints:

{
"id":"USN-4673-1",
"date":"January 04, 2021",
"project":"libproxy",
"description":"libproxy could be made to crash or execute arbitrary code if it received a specially crafted file.",
"cves":[
  "CVE-2020-26154"
],
"safeVersions":[
  {
    "ubuntu":"20.10",
    "pkg":"libproxy1v5",
    "v":"0.4.15-13ubuntu1.1"
  },
  {
    "ubuntu":"20.04",
    "pkg":"libproxy1v5",
    "v":"0.4.15-10ubuntu1.2"
  },
  {
    "ubuntu":"18.04",
    "pkg":"libproxy1v5",
    "v":"0.4.15-1ubuntu0.2"
  },
  {
    "ubuntu":"16.04",
    "pkg":"libproxy1v5",
    "v":"0.4.11-5ubuntu1.2"
  }
]}
     */
    public static void main(String[] args) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", "USN-4673-1");
        map.put("date", "January 04, 2021");
        map.put("project", "libproxy");
        map.put("description", "libproxy could be made to crash or execute arbitrary code if it received a specially crafted file.");

        ArrayList<String> cves = new ArrayList<>();
        cves.add("CVE-2020-26154");
        map.put("cves", cves);

        ArrayList<Map<String, String>> safeVersions = new ArrayList<>();
        Map<String, String> safe1 = new LinkedHashMap<>();
        safe1.put("ubuntu", "20.10");
        safe1.put("pkg", "libproxy1v5");
        safe1.put("v", "0.4.15-13ubuntu1.1");
        safeVersions.add(safe1);

        Map<String, String> safe2 = new LinkedHashMap<>();
        safe2.put("ubuntu", "20.04");
        safe2.put("pkg", "libproxy1v5");
        safe2.put("v", "0.4.15-10ubuntu1.2");
        safeVersions.add(safe2);

        Map<String, String> safe3 = new LinkedHashMap<>();
        safe3.put("ubuntu", "18.04");
        safe3.put("pkg", "libproxy1v5");
        safe3.put("v", "0.4.15-1ubuntu0.2");
        safeVersions.add(safe3);

        Map<String, String> safe4 = new LinkedHashMap<>();
        safe4.put("ubuntu", "16.04");
        safe4.put("pkg", "libproxy1v5");
        safe4.put("v", "0.4.11-5ubuntu1.2");
        safeVersions.add(safe4);
        map.put("safeVersions", safeVersions);

        System.out.println(Java2Json.format(true, map));
    }

}
