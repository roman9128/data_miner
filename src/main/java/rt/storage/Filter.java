package rt.storage;

import java.util.List;

record Filter(String whereClause, List<Object> parameters) {
}