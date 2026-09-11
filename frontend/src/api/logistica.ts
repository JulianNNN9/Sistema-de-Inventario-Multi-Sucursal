import type { ComplianceReportRow } from '../types/logistica';
import { apiClient } from './client';

interface ComplianceReportParams {
  branchId?: number;
  route?: string;
}

export async function getComplianceReport(
  params: ComplianceReportParams = {},
): Promise<ComplianceReportRow[]> {
  const { data } = await apiClient.get<ComplianceReportRow[]>('/logistics/compliance-report', {
    params,
  });
  return data;
}
