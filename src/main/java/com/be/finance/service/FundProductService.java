package com.be.finance.service;

import com.be.finance.domain.FundProductVO;
import com.be.finance.domain.ProductVO;
import com.be.finance.mapper.FundProductMapper;
import com.be.finance.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Service
public class FundProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private FundProductMapper fundProductMapper;

    // 리소스 디렉토리에서 CSV 파일을 읽어와 DB에 삽입하는 메서드
    public void importFundProduct(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true; // 첫 번째 줄(헤더) 스킵

            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // 헤더 스킵
                }

                String[] values = line.split(",");

                // 1. Product 테이블에 삽입
                ProductVO productVO = new ProductVO();
                productVO.setProductType('F'); // 펀드 타입 설정
                productMapper.insertProduct(productVO);

                int productId = productVO.getProductId();

                // 2. FundProduct 테이블에 삽입
                FundProductVO fundProductVO = new FundProductVO();
                fundProductVO.setProductId(productId);
                fundProductVO.setCompanyNm(values[0]); // 운용사명
                fundProductVO.setProductNm(values[1]); // 상품명
                fundProductVO.setYield1(new BigDecimal(values[2])); // 1개월 누적수익률
                fundProductVO.setYield3(new BigDecimal(values[3])); // 3개월 누적수익률
                fundProductVO.setYield6(new BigDecimal(values[4])); // 6개월 누적수익률
                fundProductVO.setYield12(new BigDecimal(values[5])); // 12개월 누적수익률
                fundProductVO.setRiskLevel(Integer.parseInt(values[6])); // 펀드 등급
                fundProductVO.setFundType(values[7]); //펀드 유형
                fundProductVO.setAdvancedFee(new BigDecimal(values[8])); // 선취수수료
                fundProductVO.setTotalPayoffRate(new BigDecimal(values[9])); // 총 보수율
                fundProductVO.setHit(0); // 조회수 기본값 설정

                fundProductMapper.insertFundProduct(fundProductVO);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("CSV 파일 처리 중 오류 발생: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("펀드 상품 처리 중 오류 발생: " + e.getMessage());
        }
    }
}