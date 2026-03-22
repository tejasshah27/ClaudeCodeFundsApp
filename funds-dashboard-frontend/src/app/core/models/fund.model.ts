export interface FundSummary {
  id: string;
  fundName: string;
  isin: string;
  currency: string;
  nav: number;
  status: FundStatus;
}

export interface FundDetail extends FundSummary {
  assetClass: string;
  fundManager: string;
  inceptionDate: string;
  benchmark: string;
  domicile: string;
  riskRating: number;
  aum: number;
  minInvestment: number;
  managementFee: number;
  performanceFee: number;
  distributionFrequency: string;
  fundType: string;
  legalStructure: string;
  bloombergTicker: string;
  bloombergId: string;
}

export type FundStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED';
