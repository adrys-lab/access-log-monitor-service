package com.adrian.rebollo.model;

import lombok.Getter;

@Getter
public enum AlertType {

	HIGH_TRAFFIC("ALERT - HIGH TRAFFIC generated an alert - hits = {%s}, hits/s = {%s}, triggered at {%s} , with stats from {%s}, to {%s}"),
	RECOVER("ALERT - RECOVERED from a previous HIGH TRAFFIC alert - hits = {%s}, hits/s = {%s}, triggered at {%s}, with stats from {%s}, to {%s}"),
	NO_ALERT("ALERT - NO ALERT status - hits = {%s}, hits/s = {%s}, triggered at {%s}, with stats from {%s}, to {%s}");

	private String message;

	AlertType(String message) {
		this.message = message;
	}
}
