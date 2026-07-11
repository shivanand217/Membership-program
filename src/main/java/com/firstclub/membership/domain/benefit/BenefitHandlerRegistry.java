package com.firstclub.membership.domain.benefit;

import com.firstclub.membership.domain.enums.BenefitType;
import com.firstclub.membership.domain.exception.UnknownBenefitTypeException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Indexes all {@link BenefitHandler} beans by their {@link BenefitType}. Spring injects the complete
 * list at construction, so the set of supported perks is defined purely by which handler beans exist on
 * the classpath. A duplicate registration fails fast at startup.
 */
@Component
public class BenefitHandlerRegistry {

    private final Map<BenefitType, BenefitHandler> byType;

    public BenefitHandlerRegistry(List<BenefitHandler> handlers) {
        this.byType = handlers.stream().collect(Collectors.toUnmodifiableMap(
                BenefitHandler::type,
                Function.identity(),
                (a, b) -> {
                    throw new IllegalStateException("Duplicate BenefitHandler registered for type " + a.type());
                }));
    }

    public BenefitHandler require(BenefitType type) {
        BenefitHandler handler = byType.get(type);
        if (handler == null) {
            throw new UnknownBenefitTypeException(type);
        }
        return handler;
    }

    public Set<BenefitType> registeredTypes() {
        return byType.keySet();
    }
}
