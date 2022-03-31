package com.ssafy.unique.api.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.ssafy.unique.api.request.MarketParamReq;
import com.ssafy.unique.api.request.MarketReq;
import com.ssafy.unique.api.request.MarketUpdateReq;
import com.ssafy.unique.api.response.MarketResultRes;
import com.ssafy.unique.api.response.ResultRes;
import com.ssafy.unique.db.entity.Market;
import com.ssafy.unique.db.entity.Member;
import com.ssafy.unique.db.entity.Nft;
import com.ssafy.unique.db.repository.MarketRepository;
import com.ssafy.unique.db.repository.MemberRepository;
import com.ssafy.unique.db.repository.NftRepository;

@Service
public class MarketServiceImpl implements MarketService {
	
	private final MarketRepository marketRepository;
	private final NftRepository nftRepository;
	private final MemberRepository memberRepository;
	
	public MarketServiceImpl(MarketRepository _marketRepository, NftRepository _nftRepository, MemberRepository _memberRepository) {
		this.marketRepository = _marketRepository;
		this.nftRepository = _nftRepository;
		this.memberRepository = _memberRepository;
	}
	
	private static final int SUCCESS = 1;
	private static final int FAIL = -1;

	@Override
	public MarketResultRes marketTotalSearch(MarketParamReq marketParamReq) {
		
		MarketResultRes res = new MarketResultRes();
		try {
			// SearchWord가 없는 경우
			if (marketParamReq.getSearchWord() == null || marketParamReq.getSearchWord().trim().isEmpty()) {
				// Type가 all인 경우
				if (marketParamReq.getType().length() == 3) {
					res.setMarketList(marketRepository.findTypeAllWithLimitOffset(marketParamReq.getLimit(), marketParamReq.getOffset()));
				}
				// Type가 audio, image, video인 경우
				else {
					res.setMarketList(marketRepository.findTypeOtherWithLimitOffset(marketParamReq.getType(), marketParamReq.getLimit(), marketParamReq.getOffset()));
				}
			}
			// SearchWord가 있는 경우
			else {
				// Type가 all인 경우
				if (marketParamReq.getType().length() == 3) {
					res.setMarketList(marketRepository.findTypeAllWithLimitOffsetSearchWord(marketParamReq.getSearchWord(), marketParamReq.getLimit(), marketParamReq.getOffset()));
				}
				// Type가 audio, image, video인 경우
				else {
					res.setMarketList(marketRepository.findTypeOtherWithLimitOffsetSearchWord(marketParamReq.getType(), marketParamReq.getSearchWord(), marketParamReq.getLimit(), marketParamReq.getOffset()));
				}
			}
			
			res.setResult(SUCCESS);
		} catch(Exception e) {
			e.printStackTrace();
			res.setResult(FAIL);
		}
		
		return res;
	}

	@Override
	public MarketResultRes marketDetailSearch(Long marketId) {
		
		MarketResultRes res = new MarketResultRes();
		try {
			res.setMarket(marketRepository.findById(marketId));

			res.setResult(SUCCESS);
		} catch(Exception e) {
			e.printStackTrace();
			res.setResult(FAIL);
		}
		
		return res;
	}

	@Override
	public ResultRes marketRegister(MarketReq marketReq) {
		
		ResultRes res = new ResultRes();
		
		// Security Context에서 member_Seq를 구한다
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Long memberSeq = Long.parseLong(authentication.getName());
		
		Nft nft = nftRepository.findById(marketReq.getNftSeq()).get();
		Member member = memberRepository.findById(memberSeq).get();
		
		
		
		try {
			// NFT의 소유주가 아니면 실행하지 못하도록 해야할 것
			if (memberSeq == nft.getNftOwnerSeq()) {
				
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
						
				marketRepository.save(Market.builder()
						.marketContractAddress(marketReq.getMarketContractAddress())
						.seller(member.getMemberAddress())
						.price(marketReq.getPrice())
						.startTime(LocalDateTime.parse(marketReq.getStartTime(), formatter))
						.endTime(LocalDateTime.parse(marketReq.getEndTime(), formatter))
						.nft(nft)
						.build()
						);
				res.setResult(SUCCESS);
			} else {
				System.out.println("NOT OWNER");
				res.setResult(FAIL);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
			res.setResult(FAIL);
		}
		
		return res;
	}

	@Override
	public ResultRes marketPurchase(MarketUpdateReq marketUpdateReq) {
		
		ResultRes res = new ResultRes();
		
		// Security Context에서 member_Seq를 구한다
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Long memberSeq = Long.parseLong(authentication.getName());
		
		Member member = memberRepository.findById(memberSeq).get();
		
		try {
			marketRepository.updateBuyerAndEndedById(member.getMemberAddress(), true, marketUpdateReq.getMarketId());
			nftRepository.updateOwnerSeqAndOwnerAddressById(memberSeq, member.getMemberAddress(), marketUpdateReq.getNftSeq());
			
			res.setResult(SUCCESS);
		} catch(Exception e) {
			e.printStackTrace();
			res.setResult(FAIL);
		}

		return res;
	}

	@Override
	public ResultRes marketCancel(MarketUpdateReq marketUpdateReq) {

		ResultRes res = new ResultRes();
		
		// Security Context에서 member_Seq를 구한다
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Long memberSeq = Long.parseLong(authentication.getName());
		
		Nft nft = nftRepository.findById(marketUpdateReq.getNftSeq()).get();
		
		System.out.println(memberSeq);
		
		// 해당 NFT의 주인인지 아닌지를 판단
		if (nft.getNftOwnerSeq() == memberSeq) {
			// 주인이 맞다면 취소가능
			marketRepository.updateEndedAndCanceledById(true, true, marketUpdateReq.getMarketId());
			
			res.setResult(SUCCESS);
		} else {
			System.out.println("CANNOT CANCEL: NOT OWNER");
			res.setResult(FAIL);
		}
		
		return res;
	}
	
	
}