package com.paypocket.service;

import com.paypocket.model.Currency;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Сервис курсов валют.
 *
 * <p>Возвращает обменный курс между двумя валютами и выполняет конвертацию суммы.
 * Текущая реализация использует захардкоженные курсы (упрощение для MVP),
 * в дальнейшем может быть заменена на интеграцию с внешним провайдером
 * без изменения публичного API.</p>
 */
@Service
public class ExchangeRateService {

    /**
     * Точность хранения внутреннего курса (до округления итоговой суммы).
     */
    private static final int RATE_SCALE = 8;

    /**
     * Курсы относительно базовой валюты (RUB).
     * Значение: сколько RUB стоит 1 единица данной валюты.
     */
    private static final Map<Currency, BigDecimal> RATES_TO_RUB = Map.of(
            Currency.RUB, new BigDecimal("1"),
            Currency.USD, new BigDecimal("90"),
            Currency.EUR, new BigDecimal("100")
    );

    /**
     * Возвращает курс {@code from -> to}: сколько единиц {@code to} соответствуют 1 единице {@code from}.
     */
    public BigDecimal getRate(Currency from, Currency to) {
        if (from == to) {
            return BigDecimal.ONE;
        }
        BigDecimal fromInRub = RATES_TO_RUB.get(from);
        BigDecimal toInRub = RATES_TO_RUB.get(to);
        return fromInRub.divide(toInRub, RATE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Конвертирует {@code amount} из валюты {@code from} в валюту {@code to}.
     * Результат округляется до 2 знаков (как баланс кошелька).
     */
    public BigDecimal convert(BigDecimal amount, Currency from, Currency to) {
        return amount.multiply(getRate(from, to)).setScale(2, RoundingMode.HALF_UP);
    }
}