package com.bana.dotori.account.dto;

import java.time.LocalDate;

public enum TransactionPeriod {
	WEEK {
		@Override
		public LocalDate toStartDate(LocalDate endDate) {
			return endDate.minusWeeks(1);
		}
	},
	MONTH {
		@Override
		public LocalDate toStartDate(LocalDate endDate) {
			return endDate.minusMonths(1);
		}
	},
	THREE_MONTHS {
		@Override
		public LocalDate toStartDate(LocalDate endDate) {
			return endDate.minusMonths(3);
		}
	};

	public abstract LocalDate toStartDate(LocalDate endDate);
}
