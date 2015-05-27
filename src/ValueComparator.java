package gr.phaistosnetworks.admin.otinanai;

import java.util.*;

class ValueComparator implements Comparator<Object> {

        public ValueComparator(Map<String, Integer> map) {
                this.map = map;
        }

        public int compare(Object o1, Object o2) {
                Integer v2 = map.get(o2);
                Integer v1 = map.get(o1);
                int op = v1.compareTo(v2);
                if (op == 0) 
                        return 1;
                return (op);
        }

        private Map<String, Integer> map;
}
