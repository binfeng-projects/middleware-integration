package org.springframework.batch.samples.domain.trade;

import org.springframework.batch.samples.file.multiline.Trade;

/**
 * Interface for writing a Trade object to an arbitrary output.
 *
 * @author Robert Kasanicky
 */
public interface TradeDao {

	/*
	 * Write a trade object to some kind of output, different implementations can write to
	 * file, database etc.
	 */
	void writeTrade(Trade trade);

}
