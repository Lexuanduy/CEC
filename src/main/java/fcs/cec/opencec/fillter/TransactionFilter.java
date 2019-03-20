package fcs.cec.opencec.fillter;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class TransactionFilter implements Filter {
	Logger logger = Logger.getLogger(TransactionFilter.class.getName());

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		// TODO Auto-generated method stub
		HttpServletRequest req = (HttpServletRequest) request;
		logger.info("Starting a transaction for req : {}" + req.getRequestURI());

		chain.doFilter(request, response);
		logger.info("Committing a transaction for req : {}" + req.getRequestURI());
	}

}
