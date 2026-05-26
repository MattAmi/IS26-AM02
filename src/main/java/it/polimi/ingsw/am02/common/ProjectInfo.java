package it.polimi.ingsw.am02.common;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ProjectInfo {
    public static final String GROUP      = "Group AM02";
    public static final String COURSE     = "Software Engineering — Final Project";
    public static final String UNIVERSITY = "Politecnico di Milano";
    public static final String PROFESSOR  = "Prof. Alessandro Margara";
    public static final String YEAR       = "A.Y. 2025/2026";

    public static final Map<String, String> MEMBERS;

    static {
        Map<String, String> membersMap = new LinkedHashMap<>();
        membersMap.put("Raed Abbas",        "10929713");
        membersMap.put("Matteo Amico",      "10906267");
        membersMap.put("Husnain Arshed",    "10973025");
        membersMap.put("Francesco Bagnuolo","10963509");
        MEMBERS = Collections.unmodifiableMap(membersMap);
    }

    private ProjectInfo() {}
}