package gz.httpserver.defaultcontroller;

import gz.httpserver.mustang.MustangController;
import gz.httpserver.mustang.MustangHTTPParams;

public class HelloWordContraler extends MustangController {

	public HelloWordContraler(String path) {
		super(path);
	}

	@Override
	public String onResponse(MustangHTTPParams params) throws Exception {
		return "Hello world!!!";
	}

}
