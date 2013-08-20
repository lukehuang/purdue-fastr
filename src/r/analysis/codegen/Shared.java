package r.analysis.codegen;

import java.lang.annotation.*;

/** Marker for shared node fields.
 *
 * If a field of RNode is annotated with the @Shared, it will not be deep copied, but instead will be shared by all
 * deep copies of the parent node.
 */
@Target(ElementType.FIELD)
public @interface Shared {
}
