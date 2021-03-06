package com.ssafy.unique.api.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.ssafy.unique.api.request.LoginReq;
import com.ssafy.unique.api.request.MemberBioReq;
import com.ssafy.unique.api.request.MemberReq;
import com.ssafy.unique.api.request.WalletRegisterReq;
import com.ssafy.unique.api.response.MemberRes;
import com.ssafy.unique.api.response.MemberResultRes;
import com.ssafy.unique.api.response.PopularRes;
import com.ssafy.unique.api.response.ResultRes;
import com.ssafy.unique.api.service.CustomUserDetailsService;
import com.ssafy.unique.api.service.MemberService;
import com.ssafy.unique.jwt.JwtFilter;
import com.ssafy.unique.jwt.TokenProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


@CrossOrigin(
		origins = { "http://localhost:5500", "http://172.30.1.59:5500", "http://192.168.0.100:5500", "http://192.168.0.40:5500","https://j6e205.p.ssafy.io" },
		allowCredentials = "true", // axios??? sessionId??? ?????? ???????????? ????????????, ????????? ??????????????????
		allowedHeaders = "*",
		methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, 
				RequestMethod.DELETE, RequestMethod.HEAD, RequestMethod.OPTIONS })

@RestController
@RequestMapping(value="/members")
@Tag(name = "Member Controller", description = "????????? ????????? API??? ????????????(?????????,????????????)")
public class MemberController {

	private final CustomUserDetailsService customUserDetailsService;
	private final MemberService memberService;
	private final TokenProvider tokenProvider;
	private final AuthenticationManagerBuilder authenticationManagerBuilder;

	public MemberController(CustomUserDetailsService customUserDetailsService, MemberService memberService, TokenProvider tokenProvider, AuthenticationManagerBuilder authenticationManagerBuilder) { 
		this.customUserDetailsService = customUserDetailsService; 
		this.memberService = memberService;
		this.tokenProvider = tokenProvider;
		this.authenticationManagerBuilder = authenticationManagerBuilder;
	}

	private static final int SUCCESS = 1;
	

	@Operation(description = "????????? ????????? ??????")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "????????? ??????"),
			@ApiResponse(responseCode = "401", description = "?????? ????????? ???????????? ??????. \b ?????? ??????")
	})
	@PostMapping("/login")
	public ResponseEntity<MemberRes> memberLogin(@RequestBody LoginReq loginReq) {
		System.out.println("Enter memberLogin()");
		// userId, userPassword??? ??????????????? ????????? UsernamePasswordAuthenticationToken??? ????????????
		UsernamePasswordAuthenticationToken authenticationToken =
				new UsernamePasswordAuthenticationToken(loginReq.getMemberId(), loginReq.getMemberPassword());
		System.out.println("authenticationToken : " + authenticationToken);

		// ???????????? Authentication ????????? ??????????????? authentication ???????????? ?????? ??? ???, loadUserByUsername ???????????? ????????????
		Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

		// ??????????????? ????????? ????????? SecurityContext??? ????????????
		SecurityContextHolder.getContext().setAuthentication(authentication);
		System.out.println("authentication : " + authentication);

		// ??????????????? ???????????? tokenProvider??? createToken??? ????????? JWT????????? ????????????
		String jwt = tokenProvider.createToken(authentication);
		System.out.println("jwt : " + jwt);

		// ????????? ????????? ????????????
		HttpHeaders httpHeaders = new HttpHeaders();
		
		// CORS??????
		httpHeaders.add("Access-Control-Expose-Headers", "AUTHORIZATION");
		
		httpHeaders.add(JwtFilter.AUTHORIZATION_HEADER, "Bearer " + jwt);
		System.out.println("httpHeaders : " + httpHeaders);

		// ?????? ????????? DB?????? ????????????
		MemberRes memberRes = customUserDetailsService.getMemberInfo(loginReq.getMemberId());
		
		return new ResponseEntity<MemberRes> (memberRes, httpHeaders, HttpStatus.OK);
	}

	@Operation(description = "???????????? ????????? ??????")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "???????????? ??????"),
			@ApiResponse(responseCode = "500", description = "?????? ????????? ????????? ??????. ")
	})
	@PostMapping("/register")
	public ResponseEntity<MemberResultRes> memberRegister(@ModelAttribute MemberReq memberReq, MultipartHttpServletRequest request) {
		
		MemberResultRes memberResultRes = memberService.memberRegister(memberReq, request);

		
		if (memberResultRes.getResult() == SUCCESS) {
			return new ResponseEntity<MemberResultRes>(memberResultRes, HttpStatus.OK);
		} else {
			return new ResponseEntity<MemberResultRes>(memberResultRes, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	
	@Operation(description = "????????? ??????")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "?????? ?????? ??????"),
			@ApiResponse(responseCode = "500", description = "?????? ?????? ??????")
	})
	@PutMapping("/wallet")
	public ResponseEntity<ResultRes> memberWalletRegister(@RequestBody WalletRegisterReq wallet) {
		System.out.println(wallet.getWallet());
		ResultRes resultRes = memberService.memberWalletRegister(wallet.getWallet());
		
		if (resultRes.getResult() == SUCCESS) {
			return new ResponseEntity<ResultRes> (resultRes, HttpStatus.OK);
		} else {
			return new ResponseEntity<ResultRes> (resultRes, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}
	
	@Operation(description = "????????? ????????? ????????????")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "??????"),
			@ApiResponse(responseCode = "500", description = "??????")
	})
	@PostMapping("/profileImage")
	public ResponseEntity<ResultRes> profileImageUpdate(MultipartHttpServletRequest request) {
		ResultRes res = memberService.profileImageUpdate(request);
		
		if(res.getResult() == SUCCESS) {
			return new ResponseEntity<ResultRes> (res, HttpStatus.OK);
		} else {
			return new ResponseEntity<ResultRes> (res, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}
	
	
	@Operation(description = "?????? ?????? ????????? ?????? ?????? 4?????? ??????. ??? ????????? ?????? NFT ?????? ??????")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "??????"),
			@ApiResponse(responseCode = "500", description = "??????")
	})
	@GetMapping("/popular")
	public ResponseEntity<PopularRes> popularAuthorSearch() {
		
		PopularRes res = memberService.popularAuthorSearch();
		
		if(res.getResult() == SUCCESS) {
			return new ResponseEntity<PopularRes> (res, HttpStatus.OK);
		} else {
			return new ResponseEntity<PopularRes> (res, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	
	@Operation(description = "????????? ???????????? ??????")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "??????"),
			@ApiResponse(responseCode = "500", description = "??????")
	})
	@PutMapping("/bio")
	public ResponseEntity<ResultRes> updateMemberBio(@RequestBody MemberBioReq memberBioReq) {
		
		ResultRes res = memberService.updateMemberBio(memberBioReq) ;
		
		if(res.getResult() == SUCCESS) {
			return new ResponseEntity<ResultRes> (res, HttpStatus.OK);
		} else {
			return new ResponseEntity<ResultRes> (res, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}
	
	
}













