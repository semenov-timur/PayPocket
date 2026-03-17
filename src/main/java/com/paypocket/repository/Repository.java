package com.paypocket.repository;

import java.util.List;
import java.util.Optional;

/**
 * Базовый интерфейс репозитория (CRUD-операции).
 *
 * <p>Определяет общий контракт для хранения ЛЮБЫХ сущностей.
 * Конкретные репозитории рсширяют этот интерфейс
 * дополнительными методами поиска.</p>
 *
 * @param <T> тип сущности (User, Wallet, Transaction)
 * @param <ID> тип идентификатора сущности (UUID)
 */
public interface Repository<T, ID> {

    /**
     * Сохраняет сущность.
     * Если сущность с таким id уже есть – перезаписывает.
     *
     * @param entity сущность для созранения
     * @return сохраненная сущность
     */
    T save(T entity);

    /**
     * Ищет сущность по идентификатору.
     *
     * @param id идентификатор
     * @return Optional с сущностью или пустой Optional
     */
    Optional<T> findById(ID id);

    /**
     * Возвращает все сохраненные сущности.
     *
     * @return список сохраненных сущностей (может быть пустым)
     */
    List<T> findAll();

    /**
     * Проверяет, существует ли сущность с заданным id.
     *
     * @param id идентификатор
     * @return true, если существует, иначе – false
     */
    boolean existsById(ID id);

    /**
     * Удаляет сущность по идентификатору.
     *
     * @param id идентификатор
     */
    void deleteById(ID id);

    /**
     * Возвращает количество сохраненных сущностей.
     *
     * @return количество сущностей
     */
    long  count();

}
