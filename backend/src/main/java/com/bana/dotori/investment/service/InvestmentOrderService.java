package com.bana.dotori.investment.service;

import com.bana.dotori.investment.dto.InvestmentOrderRequest;
import com.bana.dotori.investment.dto.InvestmentOrderResponse;

public interface InvestmentOrderService {

	InvestmentOrderResponse buy(Integer userId, InvestmentOrderRequest request);
}
